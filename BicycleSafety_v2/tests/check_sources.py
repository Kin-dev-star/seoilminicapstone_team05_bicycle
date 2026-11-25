# -*- coding: utf-8 -*-
"""외부 패키지 없이 실행하는 프로젝트 정적/데이터 검증."""
from pathlib import Path
import json, xml.etree.ElementTree as ET, sys
root = Path(__file__).resolve().parents[1]
xmls = list((root / 'app/src/main').rglob('*.xml'))
for p in xmls: ET.parse(p)
rows = json.loads((root/'app/src/main/assets/seoul_bicycle_accidents.json').read_text(encoding='utf-8'))['items']
assert len(rows)==2079, f'예상 행수 2079, 현재 {len(rows)}'
assert len({r['id'] for r in rows})==len(rows), '사고 ID 중복'
assert all(33<r['latitude']<39 and 124<r['longitude']<132 for r in rows), '좌표 범위 오류'
assert not (root/'local.properties').exists(), '공유 ZIP에는 local.properties가 포함되면 안 됨'
assert not any('(2)' in p.name for p in (root/'app/src/main/java').rglob('*') if p.is_file()), '중복 소스 발견'
print(f'PASS: XML {len(xmls)}개 파싱, 사고 지점 {len(rows)}개, ID/좌표 검사, 키/중복 파일 검사')
