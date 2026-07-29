import { describe, it, expect, vi } from 'vitest'
import { debounce } from '../debounce'

describe('debounce', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('延迟执行函数', () => {
    const fn = vi.fn()
    const debouncedFn = debounce(fn, 100)

    debouncedFn()
    expect(fn).not.toHaveBeenCalled()

    vi.advanceTimersByTime(100)
    expect(fn).toHaveBeenCalledTimes(1)
  })

  it('重复调用只执行最后一次', () => {
    const fn = vi.fn()
    const debouncedFn = debounce(fn, 100)

    debouncedFn()
    debouncedFn()
    debouncedFn()

    vi.advanceTimersByTime(100)
    expect(fn).toHaveBeenCalledTimes(1)
  })

  it('传递参数给原始函数', () => {
    const fn = vi.fn()
    const debouncedFn = debounce(fn, 100)

    debouncedFn('arg1', 'arg2')
    vi.advanceTimersByTime(100)

    expect(fn).toHaveBeenCalledWith('arg1', 'arg2')
  })

  it('默认延迟 300ms', () => {
    const fn = vi.fn()
    const debouncedFn = debounce(fn)

    debouncedFn()

    vi.advanceTimersByTime(299)
    expect(fn).not.toHaveBeenCalled()

    vi.advanceTimersByTime(1)
    expect(fn).toHaveBeenCalledTimes(1)
  })

  it('保持 this 上下文', () => {
    const obj = {
      value: 42,
      method: debounce(function () {
        return this.value
      }, 100)
    }

    let result
    obj.method()
    obj.method = debounce(function () {
      result = this.value
    }, 100)
    obj.method()
    vi.advanceTimersByTime(100)

    expect(result).toBe(42)
  })
})
