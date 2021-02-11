import * as Yup from "yup";

const userIdSchema = Yup.string().label("User Id").required("Required").min(3);

// const emailSchema = Yup.string().label("Email").required("Required").email();
const emailSchema = Yup.string().label("Email").notRequired().email();

// const firstNameSchema = Yup.string().label("First Name").required("Required");
const firstNameSchema = Yup.string().label("First Name").notRequired();

// const lastNameSchema = Yup.string().label("Last Name").required("Required");
const lastNameSchema = Yup.string().label("Last Name").notRequired();

export const newAccountValidationSchema = Yup.object().shape({
  // password: passwordSchema,
  // confirmPassword: confirmPasswordSchema,
  userId: userIdSchema,
  email: emailSchema,
  firstName: firstNameSchema,
  lastName: lastNameSchema,
});
