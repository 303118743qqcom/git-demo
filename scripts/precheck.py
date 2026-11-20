"""
本地静态预检：在推送/出包前捕获常见的资源与源码错误，减少无意义的 CI 往返。
不替代真实编译，但能发现绝大多数会导致 aapt2/Kotlin 编译失败的低级问题。

用法：python scripts/precheck.py
退出码 0 = 通过，1 = 发现问题。
"""
import re
import pathlib
import sys
import xml.dom.minidom as md

ROOT = pathlib.Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res"
KT = ROOT / "app/src/main/java"
errors = []

# 1. 所有 XML 良构
for p in RES.rglob("*.xml"):
    try:
        md.parse(str(p))
    except Exception as e:
        errors.append(f"XML 不良构 {p.relative_to(ROOT)}: {e}")

# 2. 颜色值格式
for p in RES.rglob("colors.xml"):
    for name, val in re.findall(r'<color\s+name="([^"]+)"\s*>([^<]*)</color>', p.read_text(encoding="utf-8")):
        v = val.strip()
        if not re.fullmatch(r"#[0-9a-fA-F]{3,8}", v):
            errors.append(f"颜色格式非法 {p.relative_to(ROOT)}: {name}={v!r}（需 #RGB/#ARGB/#RRGGBB/#AARRGGBB）")

# 3. 字符串格式化占位符（% 后必须跟 % 或 %N$）
for p in RES.rglob("strings.xml"):
    for name, val in re.findall(r'<string\s+name="([^"]+)"[^>]*>(.*?)</string>', p.read_text(encoding="utf-8"), re.S):
        for m in re.finditer(r"%(?!\d+\$[sdif]|%)", val):
            errors.append(f"字符串格式符非法 {p.relative_to(ROOT)}: {name} 含裸 % -> ...{val[max(0,m.start()-10):m.start()+12]}...")

# 4. 资源引用完整性
defined = {t: set() for t in ("string", "color", "style", "mipmap", "drawable")}
for p in RES.rglob("*.xml"):
    s = p.read_text(encoding="utf-8")
    for t in defined:
        defined[t] |= set(re.findall(rf'<{t}\s+name="([^"]+)"', s))
defined["layout"] = {p.stem for p in (RES / "layout").glob("*.xml")} if (RES / "layout").exists() else set()
refs = set()
for p in list(RES.rglob("*.xml")) + list(KT.rglob("*.kt")):
    s = p.read_text(encoding="utf-8")
    for t in ("string", "color", "layout", "style"):
        refs |= {(t, x) for x in re.findall(rf"@{t}/(\w+)", s)}
        refs |= {(t, x) for x in re.findall(rf"R\.{t}\.(\w+)", s)}
for t, name in sorted(refs):
    if t in defined and name not in defined[t] and not name.startswith("android"):
        errors.append(f"资源引用缺失 @{t}/{name}")

# 5. Kotlin 括号平衡
for p in KT.rglob("*.kt"):
    s = p.read_text(encoding="utf-8")
    for a, b in ("{}", "()", "[]"):
        if s.count(a) != s.count(b):
            errors.append(f"括号不平衡 {p.relative_to(ROOT)}: {a}{b} {s.count(a)}/{s.count(b)}")

print("=== 静态预检结果 ===")
if errors:
    for e in errors:
        print("FAIL", e)
    print(f"共 {len(errors)} 个问题")
    sys.exit(1)
print(f"ALL PASS — XML/颜色/字符串/资源引用/Kotlin括号 全部通过（字符串 {len(defined['string'])} 个）")
