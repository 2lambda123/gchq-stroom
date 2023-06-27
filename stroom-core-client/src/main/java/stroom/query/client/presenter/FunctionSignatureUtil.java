package stroom.query.client.presenter;

import stroom.dashboard.shared.FunctionSignature;
import stroom.dashboard.shared.FunctionSignature.Arg;
import stroom.dashboard.shared.FunctionSignature.Type;
import stroom.util.shared.GwtNullSafe;
import stroom.widget.menu.client.presenter.InfoMenuItem;
import stroom.widget.menu.client.presenter.Item;
import stroom.widget.menu.client.presenter.SimpleParentMenuItem;
import stroom.widget.util.client.HtmlBuilder;
import stroom.widget.util.client.HtmlBuilder.Attribute;
import stroom.widget.util.client.TableBuilder;
import stroom.widget.util.client.TableCell;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.Command;
import edu.ycp.cs.dh.acegwt.client.ace.AceCompletion;
import edu.ycp.cs.dh.acegwt.client.ace.AceCompletionSnippet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FunctionSignatureUtil {

    private static final int DEFAULT_COMPLETION_SCORE = 300; // Not sure what the range of scores is

    private FunctionSignatureUtil() {
    }

    public static List<Item> buildMenuItems(final List<FunctionSignature> signatures,
                                            final Consumer<String> insertFunction,
                                            final String helpUrlBase) {
        return buildMenuItems(signatures, insertFunction, helpUrlBase, 0);
    }

    public static List<Item> buildMenuItems(final List<FunctionSignature> signatures,
                                            final Consumer<String> insertFunction,
                                            final String helpUrlBase,
                                            final int depth) {
        // This is roughly what we are aiming for
        // Date //primary category
        //   Rounding // sub-category
        //     ceil(
        //     floor(
        //   parseDate // overload branch
        //     parseDate($) // overload 1
        //     parseDate($, $) // overload 2
        // Aggregate
        //   average(
        //   mean( // alias for average

        // leaves and overload branches come after category branches
        final Comparator<Entry<Optional<String>, List<FunctionSignature>>> entryComparator =
                Comparator.comparing(entry ->
                        entry.getKey().orElse("ZZZZ"));

        final AtomicInteger positionInMenu = new AtomicInteger(0);
        return signatures.stream()
                .collect(Collectors.groupingBy(sig -> sig.getCategory(depth)))
                .entrySet()
                .stream()
                .sorted(entryComparator)
                .flatMap(optCatSigEntry -> {
                    // Either have an empty key with a single sig in the list
                    // or a category key with one/more sigs in the list
                    final Optional<String> optCategory = optCatSigEntry.getKey();
                    final List<FunctionSignature> categorySignatures = optCatSigEntry.getValue()
                            .stream()
                            .sorted(Comparator.comparing(FunctionSignature::getName))
                            .collect(Collectors.toList());

                    if (optCategory.isPresent()) {
                        // We have a category so recurse
                        final List<Item> childItems = buildMenuItems(
                                categorySignatures,
                                insertFunction,
                                helpUrlBase,
                                depth + 1);

                        return Stream.of(new SimpleParentMenuItem(
                                positionInMenu.getAndIncrement(),
                                optCategory.get(),
                                childItems));
                    } else {
                        // No category at this depth so this is a list of leaves
                        // Due to aliases, each leaf may become multiple leaves
                        // or due to overloads each leaf may become a branch
                        final List<Item> leafItems = convertLeaves(
                                categorySignatures,
                                positionInMenu,
                                insertFunction,
                                helpUrlBase);
                        return leafItems.stream();
                    }
                })
                .collect(Collectors.toList());
    }

    private static List<Item> convertLeaves(final List<FunctionSignature> signatures,
                                            final AtomicInteger positionInMenu,
                                            final Consumer<String> insertFunction,
                                            final String helpUrlBase) {

        // Create one for each alias too, except the special one char ones
        // like +, -, /, * etc. as they have a form without brackets
        final List<FunctionSignature> categorySignatures = signatures
                .stream()
                .flatMap(value ->
                        value.asAliases().stream())
//                .peek(functionSignature ->
//                        GWT.log("Func: " +
//                                buildSignatureStr(functionSignature) + " " +
//                                isBracketedForm(functionSignature)))
                .filter(FunctionSignatureUtil::isBracketedForm)
                .sorted(Comparator.comparing(FunctionSignature::getName))
                .collect(Collectors.toList());

//        final AtomicInteger functionPosition = new AtomicInteger(0);

        // These will be branches if a func has multiple overloads in the category
        // or a leaf if it only has one
        final List<Item> childMenuItems = categorySignatures.stream()
                .collect(Collectors.groupingBy(FunctionSignature::getName))
                .entrySet()
                .stream()
                .sorted(Entry.comparingByKey())
                .map(nameSigsEntry ->
                        convertFunctionDefinitionToItem(
                                nameSigsEntry.getKey(),
                                nameSigsEntry.getValue(),
                                insertFunction,
                                positionInMenu.getAndIncrement(),
                                helpUrlBase))
                .collect(Collectors.toList());

        return childMenuItems;
    }

    public static List<AceCompletion> buildCompletions(final FunctionSignature signature,
                                                       final String helpUrlBase) {
        // FlatMap to aliases so we have one func def per alias
        // Filter on isBracketedForm to ignore aliases like +, -, * etc which have a different form,
        // e.g. 1+2 vs add(1, 2)
        return signature.asAliases()
                .stream()
                .filter(FunctionSignatureUtil::isBracketedForm)
                .map(sig ->
                        convertFunctionDefinitionToCompletion(sig, helpUrlBase))
                .collect(Collectors.toList());
    }

    public static List<AceCompletion> buildCompletions(final List<FunctionSignature> signatures,
                                                       final String helpUrlBase) {

        // FlatMap to aliases so we have one func def per alias
        // Filter on name length > 1 to ignore aliases like +, -, * etc which have a different form,
        // e.g. 1+2 vs add(1, 2)
        return signatures.stream()
                .flatMap(signature -> buildCompletions(signature, helpUrlBase).stream())
                .collect(Collectors.toList());
    }

    public static SafeHtml buildInfoHtml(final FunctionSignature signature,
                                         final String helpUrlBase) {
        if (signature != null) {
            final HtmlBuilder htmlBuilder = new HtmlBuilder();
            htmlBuilder.div(hb1 -> {
                hb1.bold(hb2 -> hb2.append(buildSignatureStr(signature)));
                hb1.br();
                hb1.hr();

                if (signature.getDescription() != null && !signature.getDescription().isEmpty()) {
                    hb1.para(hb2 -> hb2.append(signature.getDescription()),
                            Attribute.className("queryHelpDetail-description"));
                }

                final boolean addedArgs = addArgsBlockToInfo(signature, hb1);

                if (addedArgs) {
                    hb1.br();
                }

                final List<String> aliases = signature.getAliases();
                if (!aliases.isEmpty()) {
                    hb1.para(hb2 -> hb2.append("Aliases: " +
                            aliases.stream()
                                    .collect(Collectors.joining(", "))));
                }

                if (helpUrlBase != null) {
                    addHelpLinkToInfo(signature, helpUrlBase, hb1);
                }
            }, Attribute.className("queryHelpDetail"));

            return htmlBuilder.toSafeHtml();
        } else {
            return SafeHtmlUtils.EMPTY_SAFE_HTML;
        }
    }

    public static AceCompletion convertFunctionDefinitionToCompletion(
            final FunctionSignature signature,
            final String helpUrlBase) {
        return convertFunctionDefinitionToCompletion(signature, helpUrlBase, DEFAULT_COMPLETION_SCORE);
    }

    public static AceCompletion convertFunctionDefinitionToCompletion(
            final FunctionSignature signature,
            final String helpUrlBase,
            final int score) {

        final String name = buildSignatureStr(signature);

        // TODO the help link doesn't work as ace seems to be hijacking the click
        // event so leave it out for now.
        final String html = buildInfoHtml(signature, null)
                .asString();

        final String meta;
        if ("Value".equals(signature.getPrimaryCategory())) {
            meta = signature.getPrimaryCategory();
        } else if (signature.getArgs().isEmpty()) {
            meta = signature.getPrimaryCategory() + " Value";
        } else {
            meta = "Func (" + signature.getPrimaryCategory() + ")";
        }
        final String snippetText = buildSnippetText(signature);

//                    GWT.log("Adding snippet " + name + " | " + meta + " | " + snippetText);

        return new AceCompletionSnippet(
                name,
                snippetText,
                GwtNullSafe.requireNonNullElse(score, DEFAULT_COMPLETION_SCORE),
                meta,
                html);
    }

    public static String buildSnippetText(final FunctionSignature signature) {
        final String argsStr;
        if (signature.getArgs().isEmpty()) {
            argsStr = "";
        } else {
            final AtomicInteger argPosition = new AtomicInteger(1);
            final AtomicBoolean foundOptArg = new AtomicBoolean(false);
            argsStr = signature.getArgs()
                    .stream()
                    .flatMap(arg -> {
                        final List<String> snippetArgStrs = new ArrayList<>();

                        if (arg.isVarargs()) {
                            for (int i = 1; i <= arg.getMinVarargsCount(); i++) {
                                final String argName = arg.getName() + i;
                                snippetArgStrs.add(argToSnippetArg(
                                        argName,
                                        arg,
                                        argPosition.getAndIncrement()));
                            }
                        } else if (arg.isOptional()) {
                            final String name = "[" + arg.getName() + "]";
                            snippetArgStrs.add(argToSnippetArg(
                                    name,
                                    arg,
                                    argPosition.getAndIncrement()));
                        } else {
                            snippetArgStrs.add(argToSnippetArg(
                                    arg.getName(),
                                    arg,
                                    argPosition.getAndIncrement()));
                        }
                        return snippetArgStrs.stream();
                    })
                    .collect(Collectors.joining(", "));
        }

        return signature.getName() + "(" + argsStr + ")$0";
    }

    private static String argToSnippetArg(final String argName,
                                          final Arg arg,
                                          final int position) {
        final String snippetDefault = arg.getDefaultValue() != null
                ? arg.getDefaultValue()
                : argName;

//        final StringBuilder stringBuilder = new StringBuilder();
//        final boolean addQuotes = Type.STRING.equals(arg.getArgType())
//                && !snippetDefault.startsWith("${")
//                && !snippetDefault.endsWith("}");

//        if (addQuotes) {
//            stringBuilder.append("'");
//        }
        // No need to quote args as when you tab through you can surround with quotes
        // just by hitting the ' or " key. Also, more often than not, the arg value
        // is another func call or a field.
        final StringBuilder stringBuilder = new StringBuilder()
                .append("${")
                .append(position)
                .append(":")
                .append(snippetDefault
                        .replace("$", "\\$")
                        .replace("}", "\\}"))
                .append("}");
//        if (addQuotes) {
//            stringBuilder.append("'");
//        }

        return stringBuilder.toString();
    }

    private static Item convertFunctionDefinitionToItem(final String name,
                                                        final List<FunctionSignature> signatures,
                                                        final Consumer<String> insertFunction,
                                                        final int functionPosition,
                                                        final String helpUrlBase) {

        // We either return
        //   func1 (sig) -> info
        // or
        //   func1
        //     -> sig1 -> info
        //     -> sig2 -> info

        final Item functionMenuItem;
        if (signatures.size() == 1) {
            functionMenuItem = convertSignatureToItem(
                    signatures.get(0),
                    insertFunction,
                    functionPosition,
                    helpUrlBase);
        } else {
            // Multiple sigs so add a branch in the tree
            final AtomicInteger signaturePosition = new AtomicInteger(0);
            final List<Item> childItems = signatures
                    .stream()
                    .sorted(Comparator.comparing(signature -> signature.getArgs().size()))
                    .map(signature ->
                            convertSignatureToItem(
                                    signature,
                                    insertFunction,
                                    signaturePosition.getAndIncrement(),
                                    helpUrlBase))
                    .collect(Collectors.toList());

            // Wrap all the signatures in a menu item for the function
            functionMenuItem = new SimpleParentMenuItem(
                    functionPosition,
                    name + "(",
                    childItems);
        }
        return functionMenuItem;
    }

    private static Item convertSignatureToItem(final FunctionSignature signature,
                                               final Consumer<String> insertFunction,
                                               final int signaturePosition,
                                               final String helpUrlBase) {
        // Return something like
        // funcX (sigY) -> info

        final String signatureStr = buildInsertText(signature);
        final String snippetStr = buildSnippetText(signature);

        final Command command = () -> insertFunction.accept(snippetStr);
        final InfoMenuItem infoMenuItem = new InfoMenuItem(
                buildInfoHtml(signature, helpUrlBase),
                null,
                false,
                null);

        return new SimpleParentMenuItem(
                signaturePosition,
                signatureStr,
                Collections.singletonList(infoMenuItem),
                command);
    }

    public static String buildSignatureStr(final FunctionSignature signature) {
        String argsStr;
        if (signature.getArgs().isEmpty()) {
            argsStr = "()";
        } else if (signature.getArgs().size() > 3) {
            // Funcs with long arg lists get truncated. Help text explains all the args.
            argsStr = "(...";
        } else {
            final AtomicBoolean foundOptArg = new AtomicBoolean(false);
            argsStr = signature.getArgs()
                    .stream()
                    .flatMap(arg -> {
                        List<String> argStrs = new ArrayList<>();

                        if (arg.isVarargs()) {
                            for (int i = 1; i <= arg.getMinVarargsCount() + 1; i++) {
//                                final String suffix = i <= arg.getMinVarargsCount()
//                                        ? String.valueOf(i)
//                                        : "N";
//                                final String prefix = i <= arg.getMinVarargsCount()
//                                        ? ""
//                                        : "... , ";
//                                argStrs.add(prefix + arg.getName() + suffix);
                                argStrs.add(buildVarargsName(arg, i));
                            }
                        } else if (arg.isOptional() && !foundOptArg.get()) {
                            argStrs.add("[" + arg.getName());
                            foundOptArg.set(true);
                        } else {
                            argStrs.add(arg.getName());
                        }
                        return argStrs.stream();
                    })
                    .collect(Collectors.joining(", "));

            if (foundOptArg.get()) {
                argsStr += "]";
            }
            argsStr = "(" + argsStr + ")";
        }

        // Add a space to make it a bit clearer
        return signature.getName() + " " + argsStr;
    }

    private static String buildInsertText(final FunctionSignature signature) {
        String argsStr;
        final AtomicBoolean foundOptArg = new AtomicBoolean(false);
        if (signature.getArgs().isEmpty()) {
            argsStr = "";
        } else {
            argsStr = signature.getArgs()
                    .stream()
                    .flatMap(arg -> {
                        final List<String> argStrs = new ArrayList<>();

                        if (arg.isVarargs()) {
                            for (int i = 1; i <= arg.getMinVarargsCount(); i++) {
                                final String argName = arg.getName() + i;
                                argStrs.add(argName);
                            }
                        } else if (arg.isOptional() && !foundOptArg.get()) {
                            argStrs.add("[" + arg.getName());
                            foundOptArg.set(true);
                        } else {
                            argStrs.add(arg.getName());
                        }
                        return argStrs.stream();
                    })
                    .collect(Collectors.joining(", "));
        }

        if (foundOptArg.get()) {
            argsStr += "]";
        }

        return signature.getName() + "(" + argsStr + ")";
    }

    private static String buildVarargsName(final Arg arg,
                                           final int argNo) {

        final String suffix = argNo <= arg.getMinVarargsCount()
                ? String.valueOf(argNo)
                : "N";
        final String prefix = argNo <= arg.getMinVarargsCount()
                ? ""
                : "... , ";
        return prefix + arg.getName() + suffix;
    }


    private static boolean addArgsBlockToInfo(final FunctionSignature signature,
                                              final HtmlBuilder htmlBuilder) {
        AtomicBoolean addedContent = new AtomicBoolean(false);
        addedContent.set(!signature.getArgs().isEmpty());

        final TableBuilder tb = new TableBuilder();
        tb.row(
                TableCell.header("Parameter"),
                TableCell.header("Type"),
                TableCell.header("Description"));
        signature.getArgs()
                .forEach(arg -> {
                    final String argName;

                    if (arg.isVarargs()) {
                        argName = arg.getName() + "1...N";
                    } else if (arg.isOptional()) {
                        argName = "[" + arg.getName() + "]";
                    } else {
                        argName = arg.getName();
                    }

                    final StringBuilder descriptionBuilder = new StringBuilder();
                    descriptionBuilder.append(arg.getDescription());
                    if (!arg.getAllowedValues().isEmpty()) {
                        appendSpaceIfNeeded(descriptionBuilder)
                                .append("Allowed values: ")
                                .append(arg.getAllowedValues()
                                        .stream()
                                        .map(str -> "\"" + str + "\"")
                                        .collect(Collectors.joining(", ")))
                                .append(".");
                    }

                    if (arg.getDefaultValue() != null && !arg.getDefaultValue().isEmpty()) {
                        appendSpaceIfNeeded(descriptionBuilder)
                                .append("Default value: '")
                                .append(arg.getDefaultValue())
                                .append("'.");
                    }

                    if (arg.isOptional()) {
                        appendSpaceIfNeeded(descriptionBuilder)
                                .append("Optional argument.");
                    }

                    tb.row(argName,
                            convertType(arg.getArgType()),
                            descriptionBuilder.toString());
                });
        if (signature.getReturnType() != null) {
            if (!signature.getArgs().isEmpty()) {
                tb.row();
            }
            tb.row("Return",
                    convertType(signature.getReturnType()),
                    signature.getReturnDescription());
            addedContent.set(true);
        }

        htmlBuilder.div(tb::write, Attribute.className("queryHelpDetail-table"));
        return addedContent.get();
    }

    private static StringBuilder appendSpaceIfNeeded(final StringBuilder stringBuilder) {
        if (stringBuilder.length() > 0) {
            stringBuilder.append(" ");
        }
        return stringBuilder;
    }

    private static void addHelpLinkToInfo(final FunctionSignature signature,
                                          final String helpUrlBase,
                                          final HtmlBuilder htmlBuilder) {
        htmlBuilder.append("For more information see the ");
        htmlBuilder.appendLink(
                helpUrlBase +
                        signature.getPrimaryCategory().toLowerCase().replace(" ", "-") +
                        "#" +
                        functionSignatureToAnchor(signature),
                "Help Documentation");
        htmlBuilder.append(".");
    }

    private static String functionSignatureToAnchor(final FunctionSignature signature) {
        final String helpAnchor = signature.getHelpAnchor();
        if (GwtNullSafe.isBlankString(helpAnchor)) {
            return functionNameToAnchor(signature.getName());
        } else {
            return helpAnchor;
        }
    }

    private static String functionNameToAnchor(final String name) {
        final StringBuilder stringBuilder = new StringBuilder();
        for (final char chr : name.toCharArray()) {
            if (Character.isUpperCase(chr)) {
                stringBuilder.append("-")
                        .append(String.valueOf(chr).toLowerCase());
            } else {
                stringBuilder.append(chr);
            }
        }
        return stringBuilder.toString();
    }

    private static String convertType(final Type type) {
        final String number = "Number";
        switch (type) {
            case LONG:
            case DOUBLE:
            case INTEGER:
            case NUMBER:
                return number;
            case STRING:
                return "Text";
            default:
                return type.getName();
        }
    }

    public static boolean isBracketedForm(final FunctionSignature signature) {

        return signature.getName().length() > 1
                && Character.isLetter(signature.getName().charAt(0));
    }

}
