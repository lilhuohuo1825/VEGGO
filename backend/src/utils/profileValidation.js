const PHONE_REGEX = /^0\d{9}$/;
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function validateProfileInput({ name, phone, email }) {
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

  return {
    valid: true,
    data: {
      name: trimmedName,
      phone: trimmedPhone,
      email: trimmedEmail,
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
  };
}

module.exports = {
  validateProfileInput,
  formatProfileResponse,
};
