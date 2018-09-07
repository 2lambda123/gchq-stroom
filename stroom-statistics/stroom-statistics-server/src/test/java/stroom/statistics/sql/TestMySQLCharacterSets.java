/*
 * Copyright 2016 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.statistics.sql;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

/**
 * Test class to explore issues with character sets between Java and MySql.
 * Currently ignored as most of the tests require human oversight. Left in, in
 * case it proves useful.
 */
@Ignore
public class TestMySQLCharacterSets {
    @BeforeClass
    public static void setup() throws SQLException {
        try (final Connection connection = getConnection()) {
            try (final PreparedStatement preparedStatement = connection.prepareStatement("DROP TABLE IF EXISTS CHAR_SET_TEST")) {
                preparedStatement.execute();
            }
            try (final PreparedStatement preparedStatement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS CHAR_SET_TEST (TXT VARCHAR(255)) ENGINE=InnoDB AUTO_INCREMENT=129 DEFAULT CHARSET=latin1")) {
                preparedStatement.execute();
            }
        }
    }

    @Test
    public void testRegexOnNotSign() throws SQLException {
        final String tabCharValue = "a¬b";
        final String tabCharRegex = "a[¬]b(¬|$)";
        final String insertBit = "INSERT INTO CHAR_SET_TEST VALUES (";

        try (final Connection connection = getConnection()) {
            // prepared statement escape chars for us
            try (final PreparedStatement preparedStatement = connection.prepareStatement(insertBit + "?)")) {
                preparedStatement.setString(1, tabCharValue);
                preparedStatement.execute();
            }

            try (final PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM CHAR_SET_TEST WHERE TXT REGEXP ?")) {
                preparedStatement.setString(1, tabCharRegex);

                int i = 0;
                String result = "";
                try (final ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        result = resultSet.getString(1);
                        i++;
                    }
                }
                Assert.assertEquals(1, i);
                Assert.assertEquals(tabCharValue, result);
            }
        }
    }

    @Test
    public void testEscapingTab() throws SQLException {
        final String vTab = new String(new byte[]{11});
        final String tabCharValue = "a" + vTab + "b" + vTab;
        final String tabCharRegex = "a[" + vTab + "]b(" + vTab + "|$)";
        final String insertBit = "INSERT INTO CHAR_SET_TEST VALUES (";

        try (final Connection connection = getConnection()) {
            // prepared statement escape chars for us
            try (final PreparedStatement preparedStatement = connection.prepareStatement(insertBit + "?)")) {
                preparedStatement.setString(1, tabCharValue);
                preparedStatement.execute();
            }

            try (final PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM CHAR_SET_TEST WHERE TXT REGEXP ?")) {
                preparedStatement.setString(1, tabCharRegex);

                int i = 0;
                String result = "";
                try (final ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        result = resultSet.getString(1);
                        i++;
                    }
                }

                Assert.assertEquals(1, i);
                Assert.assertEquals(tabCharValue, result);
            }
        }
    }

    @Test
    public void testEscaping() throws SQLException {
        final String dirtyValue = "\\_\"_'";
        final String insertBit = "INSERT INTO CHAR_SET_TEST VALUES (";

        try (final Connection connection = getConnection()) {
            // prepared statement escape chars for us
            try (final PreparedStatement preparedStatement = connection.prepareStatement(insertBit + "?)")) {
                preparedStatement.setString(1, dirtyValue);
                preparedStatement.execute();
            }

            // statement runs what it is given so need to escape dirty chars
            final String safeValue = SQLSafe.escapeChars(dirtyValue);
            try (final Statement statement = connection.createStatement()) {
                statement.execute(insertBit + "'" + safeValue + "')");
            }
        }
    }

    @Test
    public void testDelimiter() throws SQLException {
        try (final Connection connection = getConnection()) {
            try (final PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO CHAR_SET_TEST VALUES (?)")) {
                preparedStatement.setString(1, "a¬b¬c¬d");
                preparedStatement.execute();
            }

            try (final PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO CHAR_SET_TEST VALUES (?)")) {
                preparedStatement.setString(1, "a\u00acb\u00acc\u00acd");
                preparedStatement.execute();
            }

            dumpAllRows();
        }
    }

    @Test
    public void testString() {
        final String delim = "¬";
        final Charset charset = Charset.forName("ISO8859_1");
        final byte[] latinBytes = delim.getBytes(charset);
        final String latin1 = new String(latinBytes, charset);
        final Map<String, Charset> charsets = Charset.availableCharsets();
        System.out.println(charsets.keySet());
        System.out.println(latin1);
    }

    private void dumpAllRows() throws SQLException {
        try (final Connection connection = getConnection()) {
            try (final PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM CHAR_SET_TEST")) {
                int i = 0;
                System.out.println("---Results---");
                try (final ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        final String result = resultSet.getString(1);
                        System.out.println(result);
                        i++;
                    }
                }
                System.out.println("-------------");
            }
        }
    }

    private static Connection getConnection() throws SQLException {
        final String driverClassname = "com.mysql.jdbc.Driver";
        final String driverUrl = "jdbc:mysql://localhost/stroom";
        final String driverUsername = System.getProperty("user.name");
        final String driverPassword = "password";

        try {
            Class.forName(driverClassname);
        } catch (final ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        return DriverManager.getConnection(driverUrl, driverUsername, driverPassword);
    }
}
