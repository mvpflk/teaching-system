import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useQuestionBasketStore } from '@/stores/questionBasket'

describe('questionBasket store', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
  })

  it('add/toggle/has/remove/clear 基础语义', () => {
    const b = useQuestionBasketStore()
    b.add(1); b.add(2); b.add(1)
    expect(b.count).toBe(2)
    expect(b.has(1)).toBe(true)
    b.toggle(1)
    expect(b.has(1)).toBe(false)
    b.toggle(1)
    expect(b.has(1)).toBe(true)
    b.remove(1)
    expect(b.count).toBe(1)
    b.clear()
    expect(b.count).toBe(0)
  })

  it('difficultyDist 基于 hydrated 数据统计三档', () => {
    const b = useQuestionBasketStore()
    b.add(1); b.add(2); b.add(3)
    b.hydrated = { 1: { difficultyLevel: 1 }, 2: { difficultyLevel: 3 }, 3: { difficultyLevel: 3 } }
    expect(b.difficultyDist).toEqual({ 1: 1, 2: 0, 3: 2 })
  })

  it('ids 持久化到 localStorage 并按 userId 隔离', () => {
    const b = useQuestionBasketStore()
    b.init(42)
    b.add(7)
    expect(localStorage.getItem('qb_basket:42')).toBe('[7]')
    b.init(43)
    expect(b.count).toBe(0)
  })

  it('init null uid 不执行', () => {
    const b = useQuestionBasketStore()
    b.init(null)
    expect(b.initialized).toBe(false)
  })

  it('clear 重置 hydrateError', () => {
    const b = useQuestionBasketStore()
    b.add(1)
    b.hydrateError = true
    b.clear()
    expect(b.hydrateError).toBe(false)
  })
})
