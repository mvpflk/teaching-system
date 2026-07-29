export function useFormRules() {
  const required = (label, trigger = 'blur') => ({
    required: true, message: `请输入${label}`, trigger,
  })
  const selectRequired = (label, trigger = 'change') => ({
    required: true, message: `请选择${label}`, trigger,
  })
  const arrayRequired = (label) => ({
    type: 'array', required: true, message: `请选择${label}`, trigger: 'change',
  })
  const maxLength = (max, label) => ({
    max, message: `${label}最多 ${max} 个字符`, trigger: 'blur',
  })
  const minLength = (min, label) => ({
    min, message: `${label}至少 ${min} 个字符`, trigger: 'blur',
  })
  const email = () => ({
    type: 'email', message: '邮箱格式不正确', trigger: 'blur',
  })
  const phone = () => ({
    pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur',
  })
  const numberRange = (min, max) => ({
    type: 'number', min, max, message: `范围 ${min}~${max}`, trigger: 'blur',
  })

  return { required, selectRequired, arrayRequired, maxLength, minLength, email, phone, numberRange }
}
