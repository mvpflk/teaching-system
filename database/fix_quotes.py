import re

with open('/tmp/v217.sql', 'r', encoding='utf-8') as f:
    content = f.read()

# Split into individual UPDATE statements and fix each
parts = content.split("SET content = '")
header = parts[0]
rest = parts[1:]

fixed_parts = [header]
for part in rest:
    # Find the closing ' WHERE id = XXX;
    idx = part.rfind("' WHERE")
    if idx == -1:
        fixed_parts.append("SET content = '" + part)
        continue
    body = part[:idx]
    tail = part[idx:]
    # Double all single quotes in body
    body_fixed = body.replace("'", "''")
    fixed_parts.append("SET content = '" + body_fixed + tail)

result = ''.join(fixed_parts)

with open('/tmp/v217_fixed.sql', 'w', encoding='utf-8') as f:
    f.write(result)
print(f"Fixed {len(parts)-1} UPDATE statements")
