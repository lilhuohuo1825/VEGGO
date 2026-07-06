const PHONE_REGEX = /^0\d{9}$/;
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const BIRTHDAY_REGEX = /^\d{2}\/\d{2}\/\d{4}$/;

function normalizeGender(raw) {
  const value = String(raw || '').trim().toLowerCase();
  if (!value) {
    return '';
  }
  if (value === 'nam' || value === 'male') {
    return 'Nam';
  }
  if (value === 'nữ' || value === 'nu' || value === 'female') {
    return 'Nữ';
  }
  return null;
}

function validateBirthday(raw) {
  const trimmed = String(raw || '').trim();
  if (!trimmed) {
    return { valid: true, value: '' };
  }
  if (!BIRTHDAY_REGEX.test(trimmed)) {
    return { valid: false, message: 'Ngày sinh không đúng định dạng (dd/MM/yyyy)' };
  }
  const [day, month, year] = trimmed.split('/').map(Number);
  const date = new Date(year, month - 1, day);
  if (
    date.getFullYear() !== year
    || date.getMonth() !== month - 1
    || date.getDate() !== day
  ) {
    return { valid: false, message: 'Ngày sinh không hợp lệ' };
  }
  if (date > new Date()) {
    return { valid: false, message: 'Ngày sinh không thể ở tương lai' };
  }
  return { valid: true, value: trimmed };
}

function validateProfileInput({ name, phone, email, birthday, gender }) {
  const trimmedName = String(name || '').trim();
  const trimmedPhone = String(phone || '').trim();
  const trimmedEmail = String(email || '').trim();

  if (!trimmedName) {
    return { valid: false, status: 400, message: 'Họ và tên không được để trống' };
  }

  if (!PHONE_REGEX.test(trimmedPhone)) {
    return {
      valid: false,
      status: 400,
      message: 'Số điện thoại phải bắt đầu bằng 0 và gồm đúng 10 chữ số',
    };
  }

  if (trimmedEmail && !EMAIL_REGEX.test(trimmedEmail)) {
    return { valid: false, status: 400, message: 'Email không hợp lệ' };
  }

  const birthdayResult = validateBirthday(birthday);
  if (!birthdayResult.valid) {
    return { valid: false, status: 400, message: birthdayResult.message };
  }

  const normalizedGender = normalizeGender(gender);
  if (gender && String(gender).trim() && normalizedGender === null) {
    return { valid: false, status: 400, message: 'Giới tính không hợp lệ' };
  }

  return {
    valid: true,
    data: {
      name: trimmedName,
      phone: trimmedPhone,
      email: trimmedEmail,
      birthday: birthdayResult.value,
      gender: normalizedGender || '',
    },
  };
}

function formatProfileResponse(user) {
  return {
    id: String(user._id),
    name: user.FullName || user.name || '',
    phone: user.Phone || user.phone || '',
    email: user.Email || user.email || '',
    avatarUrl: user.avatarUrl || '',
    birthday: user.BirthDay || '',
    gender: user.Gender || '',
  };
}

module.exports = {
  validateProfileInput,
  formatProfileResponse,
};
