/**
 * Win7 CMD 命令定义工厂
 * 接收 win7Sim store 引用 + currentDir ref，返回所有命令的处理函数
 * 包括：文件系统命令 + 网络诊断命令
 */

// ── ping 输出辅助函数 ──
function pingOut(target, resolvedIp, { ttl, time, loss = 0 }) {
  const lines = []
  const recv = loss === 100 ? '0' : '4'
  const lossRate = loss === 100 ? '100%' : '0%'
  lines.push({ type: 'output', text: `正在 Ping ${target} [${resolvedIp}] 具有 32 字节的数据:` })
  for (let i = 0; i < 4; i++) {
    if (loss === 100) {
      lines.push({ type: 'output', text: '请求超时。' })
    } else {
      const ms = time.startsWith('<') ? '<1ms' : `${Math.floor(parseInt(time) * (0.8 + Math.random() * 0.4))}ms`
      lines.push({ type: 'output', text: `来自 ${resolvedIp} 的回复: 字节=32 时间=${ms} TTL=${ttl}` })
    }
  }
  lines.push({ type: 'output', text: '' })
  lines.push({ type: 'output', text: `${resolvedIp} 的 Ping 统计信息:` })
  lines.push({ type: 'output', text: `    数据包: 已发送 = 4，已接收 = ${recv}，丢失 = ${4 - parseInt(recv)} (${lossRate} 丢失)，` })
  return lines
}

export function createWin7Commands(store, currentDirRef) {
  // ── 预先定义的辅助函数（供别名引用）──
  function _ipconfig(args) {
    const config = store.networkConfig
    const flag = (args || '').toLowerCase()
    const lines = [
      { type: 'output', text: 'Windows IP 配置' },
      { type: 'output', text: '' },
      { type: 'output', text: '以太网适配器 本地连接:' },
      { type: 'output', text: '   连接特定的 DNS 后缀 . . . . . . :' },
      { type: 'output', text: `   IPv4 地址 . . . . . . . . . . . . : ${config.localIP}` },
      { type: 'output', text: `   子网掩码  . . . . . . . . . . . . : ${config.subnetMask}` },
      { type: 'output', text: `   默认网关. . . . . . . . . . . . . : ${config.gateway || '(空)'}` }
    ]
    if (flag === '/all') {
      lines.push({ type: 'output', text: `   主机名  . . . . . . . . . . . . . : Student-PC` })
      lines.push({ type: 'output', text: `   DHCP 已启用 . . . . . . . . . . . : ${config.dhcp ? '是' : '否'}` })
      lines.push({ type: 'output', text: `   DNS 服务器  . . . . . . . . . . . : ${config.dns}` })
      lines.push({ type: 'output', text: `   物理地址. . . . . . . . . . . . . : ${config.mac}` })
    }
    return lines
  }

  function _tracert(args) {
    if (!args) return [{ type: 'error', text: '用法: tracert [-d] [-h 最大跃点] <目标>' }]
    const config = store.networkConfig
    let target = args, noResolve = false, maxHops = 30
    if (target.startsWith('-d ')) { noResolve = true; target = target.substring(3).trim() }
    if (target.startsWith('-h ')) {
      const p = target.split(' ').filter(Boolean)
      maxHops = parseInt(p[1]) || 30
      target = p.slice(2).join(' ')
    }
    if (!config.remoteHosts?.[target]) return [{ type: 'error', text: `无法解析目标系统名称 ${target}。` }]
    const hops = [
      { hop: 1, ip: config.gateway, t1: '<1ms', t2: '<1ms', t3: '<1ms', name: ' gateway.local' },
      { hop: 2, ip: '10.0.0.1', t1: '1ms', t2: '1ms', t3: '1ms', name: '' },
      { hop: 3, ip: '172.16.0.1', t1: '5ms', t2: '4ms', t3: '5ms', name: '' }
    ]
    if (/[a-z]/i.test(target) || target === '8.8.8.8') {
      hops.push({ hop: 4, ip: '61.135.169.125', t1: '12ms', t2: '11ms', t3: '13ms', name: '' })
      hops.push({ hop: 5, ip: '220.181.38.148', t1: '15ms', t2: '14ms', t3: '16ms', name: '' })
    }
    if (maxHops < hops.length) hops.length = maxHops
    const lines = [
      { type: 'output', text: `通过最多 ${maxHops} 个跃点跟踪` },
      { type: 'output', text: `到 ${target} 的路由:` }, { type: 'output', text: '' }
    ]
    for (const h of hops) {
      lines.push({ type: 'output', text: `  ${h.hop}    ${h.t1}   ${h.t2}   ${h.t3}  ${h.ip}${noResolve ? '' : h.name}` })
    }
    lines.push({ type: 'output', text: '' }, { type: 'output', text: '跟踪完成。' })
    return lines
  }

  function _netstat(args) {
    const flag = (args || '').toLowerCase()
    const lines = [
      { type: 'output', text: '活动连接' }, { type: 'output', text: '' },
      { type: 'output', text: '  协议    本地地址                外部地址                状态' }
    ]
    const conns = [
      ['TCP', '192.168.1.100:443', '220.181.38.148:443', 'ESTABLISHED'],
      ['TCP', '192.168.1.100:80', '110.242.68.66:80', 'TIME_WAIT']
    ]
    if (flag.includes('-a') || flag.includes('-an')) {
      conns.push(
        ['TCP', '0.0.0.0:80', '0.0.0.0:0', 'LISTENING'],
        ['TCP', '0.0.0.0:443', '0.0.0.0:0', 'LISTENING'],
        ['TCP', '0.0.0.0:135', '0.0.0.0:0', 'LISTENING'],
        ['TCP', '0.0.0.0:445', '0.0.0.0:0', 'LISTENING'],
        ['UDP', '0.0.0.0:53', '*:*', '']
      )
    }
    for (const c of conns) {
      const pid = flag.includes('-o') ? ` [PID:${Math.floor(1000 + Math.random() * 5000)}]` : ''
      lines.push({ type: 'output', text: `  ${c[0].padEnd(6)} ${c[1].padEnd(22)} ${c[2].padEnd(22)} ${c[3]}${pid}` })
    }
    return lines
  }

  // ── 构建命令对象 ──
  const driveCommands = {
    'c:': (args) => { currentDirRef.value = 'C:\\'; return []; },
    'd:': (args) => { currentDirRef.value = 'D:\\'; return []; },
    'a:': (args) => { currentDirRef.value = 'A:\\'; return []; },
    'e:': (args) => { currentDirRef.value = 'E:\\'; return []; },
    'f:': (args) => { currentDirRef.value = 'F:\\'; return []; },
    'c:\\': (args) => { currentDirRef.value = 'C:\\'; return []; },
    'd:\\': (args) => { currentDirRef.value = 'D:\\'; return []; }
  }

  return {
    ...driveCommands,
    // ═══ 文件系统命令 ═══
    dir: () => {
      const cur = currentDirRef.value
      const node = store.findNode(cur)
      if (!node?.children) return [{ type: 'error', text: '系统找不到指定的路径。' }]
      const lines = [
        { type: 'output', text: ' 驱动器 C 中的卷没有标签。' },
        { type: 'output', text: ` ${cur} 的目录` }, { type: 'output', text: '' }
      ]
      for (const c of node.children) {
        lines.push({ type: 'output', text: ` ${(c.type === 'folder' || c.type === 'drive') ? '<DIR>' : '     '}    ${c.name}` })
      }
      lines.push({ type: 'output', text: `               ${node.children.length} 个文件` })
      return lines
    },

    cd: (args) => {
      if (!args) { currentDirRef.value = 'C:\\Users\\Student'; return [] }
      if (args === '..') {
        const parts = currentDirRef.value.split('\\')
        if (parts.length > 1) { parts.pop(); currentDirRef.value = parts.join('\\') }
        return []
      }
      const target = currentDirRef.value + '\\' + args
      const node = store.findNode(target)
      if (!node || node.type === 'file') return [{ type: 'error', text: '系统找不到指定的路径。' }]
      currentDirRef.value = target
      return []
    },

    md: (args) => {
      if (!args) return [{ type: 'error', text: '命令语法不正确。' }]
      const path = currentDirRef.value + '\\' + args
      const parentPath = path.replace(/\\[^\\]+$/, '')
      const name = path.split('\\').pop()
      return store.createFolder(parentPath, name)
        ? [{ type: 'output', text: ` 目录创建成功: ${name}` }]
        : [{ type: 'error', text: '目录已存在或路径无效。' }]
    },

    rd: (args) => {
      if (!args) return [{ type: 'error', text: '命令语法不正确。' }]
      const path = currentDirRef.value + '\\' + args
      return store.deleteFile(path)
        ? [{ type: 'output', text: ` 目录已删除: ${args}` }]
        : [{ type: 'error', text: '系统找不到指定的目录。' }]
    },

    copy: (args) => {
      if (!args) return [{ type: 'error', text: '命令语法不正确。' }]
      const parts = args.split(/\s+/)
      if (parts.length < 2) return [{ type: 'error', text: '命令语法不正确。' }]
      const src = (parts[0].includes(':') || parts[0].startsWith('\\')) ? parts[0] : currentDirRef.value + '\\' + parts[0]
      const dst = (parts[1].includes(':') || parts[1].startsWith('\\')) ? parts[1] : currentDirRef.value + '\\' + parts[1]
      return store.copyFile(src, dst)
        ? [{ type: 'output', text: ' 已复制 1 个文件。' }]
        : [{ type: 'error', text: '系统找不到指定的文件。' }]
    },

    del: (args) => {
      if (!args) return [{ type: 'error', text: '命令语法不正确。' }]
      const path = currentDirRef.value + '\\' + args
      return store.deleteFile(path)
        ? [{ type: 'output', text: ` 文件已删除: ${args}` }]
        : [{ type: 'error', text: '系统找不到指定的文件。' }]
    },

    ren: (args) => {
      if (!args) return [{ type: 'error', text: '命令语法不正确。' }]
      const parts = args.split(/\s+/)
      if (parts.length < 2) return [{ type: 'error', text: '命令语法不正确。' }]
      const path = currentDirRef.value + '\\' + parts[0]
      return store.renameFile(path, parts[1])
        ? [{ type: 'output', text: ` 文件已重命名为: ${parts[1]}` }]
        : [{ type: 'error', text: '系统找不到指定的文件。' }]
    },

    cls: () => [],
    help: () => [
      { type: 'output', text: '可用命令: dir cd md rd copy del ren type cls help date time exit tree attrib' },
      { type: 'output', text: '网络命令: ping ipconfig tracert netstat nslookup netsh' }
    ],
    date: () => [{ type: 'output', text: `当前日期: ${new Date().toLocaleDateString('zh-CN')}` }],
    time: () => [{ type: 'output', text: `当前时间: ${new Date().toLocaleTimeString('zh-CN')}` }],

    type: (args) => {
      if (!args) return [{ type: 'error', text: '命令语法不正确。' }]
      const path = currentDirRef.value + '\\' + args
      const node = store.findNode(path)
      if (!node || node.type !== 'file') return [{ type: 'error', text: '系统找不到指定的文件。' }]
      return [{ type: 'output', text: node.content || `[${node.name} 的内容]` }]
    },

    tree: (args) => {
      const dir = args || currentDirRef.value
      const node = store.findNode(dir)
      if (!node?.children) return [{ type: 'error', text: '系统找不到指定的路径。' }]
      function buildTree(n, prefix = '') {
        const lines = [{ type: 'output', text: prefix + n.name }]
        if (n.children) n.children.forEach((c, i) => {
          lines.push(...buildTree(c, prefix + (i === n.children.length - 1 ? '└── ' : '├── ')))
        })
        return lines
      }
      return buildTree(node)
    },

    attrib: (args) => {
      if (!args) return [{ type: 'error', text: '命令语法不正确。' }]
      const path = currentDirRef.value + '\\' + args
      const node = store.findNode(path)
      if (!node) return [{ type: 'error', text: '系统找不到指定的文件。' }]
      return [{ type: 'output', text: `A    ${node.hidden ? 'H' : ' '}    ${node.readonly ? 'R' : ' '}    ${path}` }]
    },

    exit: () => {
      const win = store.openWindows.find(w => w.app === 'cmd')
      if (win) store.closeWindow(win.id)
      return []
    },

    // ═══ 网络诊断命令 ═══
    ping: (args) => {
      if (!args) return [{ type: 'error', text: '用法: ping [-t] <目标地址>' }]
      const isContinuous = args.startsWith('-t ')
      const target = isContinuous ? args.substring(3).trim() : args.trim()
      const config = store.networkConfig
      if (target === '127.0.0.1' || target === 'localhost') {
        return pingOut(target, '127.0.0.1', { ttl: 128, time: '<1ms', loss: 0 })
      }
      const status = config.remoteHosts?.[target]
      if (!status) {
        return [
          { type: 'output', text: `正在 Ping ${target} 具有 32 字节的数据:` },
          { type: 'error', text: `Ping 请求找不到主机 ${target}。请检查该名称，然后重试。` }
        ]
      }
      if (status === 'offline') return pingOut(target, target, { ttl: 0, time: '-', loss: 100 })
      const isDomain = /[a-z]/i.test(target) && !/^\d/.test(target)
      const resolvedIp = isDomain ? '220.181.38.148' : target
      const ttl = isDomain ? 54 : 64
      const baseTime = target === config.gateway ? '<1ms' : (10 + Math.floor(Math.random() * 20)) + 'ms'
      return pingOut(target, resolvedIp, { ttl, time: baseTime, loss: 0 })
    },

    ipconfig: _ipconfig,
    tracert: _tracert,
    netstat: _netstat,

    nslookup: (args) => {
      if (!args) return [{ type: 'error', text: '用法: nslookup <域名> [DNS服务器]' }]
      const parts = args.split(/\s+/)
      const domain = parts[0]
      const dnsServer = parts[1]
      const config = store.networkConfig
      const lines = [
        { type: 'output', text: `服务器:  dns.google` },
        { type: 'output', text: `Address:  ${dnsServer || config.dns}` },
        { type: 'output', text: '' }
      ]
      const mockIPs = { 'www.baidu.com': ['110.242.68.66', '110.242.68.67'], 'mail.163.com': ['123.58.180.8'] }
      lines.push({ type: 'output', text: `名称:    ${domain}` })
      for (const ip of (mockIPs[domain] || ['127.0.0.1'])) {
        lines.push({ type: 'output', text: `Address:  ${ip}` })
      }
      return lines
    },

    netsh: (args) => {
      if (!args) return [{ type: 'error', text: '用法: netsh interface ip ...' }]
      const config = store.networkConfig
      const fullArgs = args.toLowerCase()
      if (fullArgs.includes('set') && fullArgs.includes('address') && fullArgs.includes('static')) {
        const ipMatch = args.match(/(\d+\.\d+\.\d+\.\d+)/g)
        if (ipMatch && ipMatch.length >= 2) {
          config.localIP = ipMatch[0]
          config.subnetMask = ipMatch[1]
          if (ipMatch.length >= 3) config.gateway = ipMatch[2]
          config.dhcp = false
          return [{ type: 'output', text: '确定。' }]
        }
        return [{ type: 'error', text: '参数格式不正确。' }]
      }
      if (fullArgs.includes('set') && fullArgs.includes('dhcp')) {
        config.localIP = '192.168.1.100'; config.gateway = '192.168.1.1'; config.dhcp = true
        return [{ type: 'output', text: '确定。' }]
      }
      if (fullArgs.includes('set') && fullArgs.includes('dns')) {
        const ipMatch = args.match(/(\d+\.\d+\.\d+\.\d+)/g)
        if (ipMatch) { config.dns = ipMatch[0]; return [{ type: 'output', text: '确定。' }] }
        return [{ type: 'error', text: 'DNS 服务器地址格式不正确。' }]
      }
      return [{ type: 'output', text: '命令执行成功。' }]
    },

    // ═══ ipconfig 子命令别名（引用预先定义的 _ipconfig）═══
    'ipconfig /all': () => _ipconfig('/all'),
    'ipconfig /release': () => {
      store.networkConfig.localIP = '0.0.0.0'
      store.networkConfig.gateway = ''
      return [
        { type: 'output', text: 'Windows IP 配置' }, { type: 'output', text: '' },
        { type: 'output', text: '以太网适配器 本地连接:' },
        { type: 'output', text: '   连接特定的 DNS 后缀 . . . . . . :' },
        { type: 'output', text: '   已释放 IP 地址。' }
      ]
    },
    'ipconfig /renew': () => {
      store.networkConfig.localIP = '192.168.1.100'
      store.networkConfig.gateway = '192.168.1.1'
      return [
        { type: 'output', text: 'Windows IP 配置' }, { type: 'output', text: '' },
        { type: 'output', text: '以太网适配器 本地连接:' },
        { type: 'output', text: '   连接特定的 DNS 后缀 . . . . . . :' },
        { type: 'output', text: `   IPv4 地址 . . . . . . . . . . . . : ${store.networkConfig.localIP}` },
        { type: 'output', text: '   IP 地址租约已成功续订。' }
      ]
    },
    'ipconfig /flushdns': () => [
      { type: 'output', text: 'Windows IP 配置' }, { type: 'output', text: '' },
      { type: 'output', text: '已成功刷新 DNS 解析缓存。' }
    ]
  }
}
