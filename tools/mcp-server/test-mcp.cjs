const { spawn } = require('child_process');
const server = spawn('node', ['dist/index.js'], {
  cwd: 'D:/TEACH/teaching-system/tools/mcp-server',
  stdio: ['pipe', 'pipe', 'pipe'],
  env: { ...process.env, DB_HOST: '127.0.0.1', DB_PORT: '3306', DB_USER: 'root', DB_PASSWORD: 'root123', DB_NAME: 'teaching_system' }
});

let output = '';
server.stdout.on('data', d => { output += d.toString(); });
server.stderr.on('data', d => { });

const tests = [
  {id:1, name:'班级列表', tool:'teaching_classes', args:{}},
  {id:2, name:'学生数量', tool:'db_count', args:{table:'students'}},
  {id:3, name:'教师数量', tool:'db_count', args:{table:'teachers'}},
  {id:4, name:'题库状态分布', tool:'db_query', args:{sql:'SELECT status, COUNT(*) as cnt FROM question_bank GROUP BY status'}},
  {id:5, name:'题型分布', tool:'db_query', args:{sql:'SELECT question_type, COUNT(*) as cnt FROM question_bank WHERE status=1 GROUP BY question_type'}},
  {id:6, name:'知识点节点数', tool:'db_count', args:{table:'knowledge_nodes'}},
  {id:7, name:'任务数量', tool:'db_count', args:{table:'tasks'}},
  {id:8, name:'待审核题目数', tool:'db_query', args:{sql:'SELECT COUNT(*) as cnt FROM question_bank WHERE status=0'}},
];

tests.forEach((t, i) => {
  setTimeout(() => {
    const req = JSON.stringify({jsonrpc:'2.0',id:t.id,method:'tools/call',params:{name:t.tool,arguments:t.args}}) + '\n';
    server.stdin.write(req);
  }, i * 300);
});

setTimeout(() => {
  server.kill();
  const lines = output.split('\n').filter(l => l.startsWith('{'));
  lines.forEach(l => {
    try {
      const r = JSON.parse(l);
      const t = tests.find(x => x.id === r.id);
      const text = r.result?.content?.[0]?.text || 'N/A';
      let parsed;
      try { parsed = JSON.parse(text); } catch(e) { parsed = text; }
      const label = t ? t.name : 'ID-'+r.id;
      if (Array.isArray(parsed)) {
        console.log(label + ': ' + parsed.length + '条');
        if (parsed.length > 0) console.log('  首条:', JSON.stringify(parsed[0]).substring(0, 150));
      } else {
        console.log(label + ':', JSON.stringify(parsed).substring(0, 200));
      }
    } catch(e) {}
  });
}, 5000);
