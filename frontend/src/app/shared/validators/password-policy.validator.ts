import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export function passwordPolicyValidator(min = 15, max = 64): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value as string | null | undefined;

    if (value == null || value.length === 0) {
      return null;
    }

    if (value.length < min) {
      return { passwordTooShort: true };
    }

    if (value.length > max) {
      return { passwordTooLong: true };
    }

    return null;
  };
}