# noun_extractor.py

import json
from konlpy.tag import Okt

def extract_nouns_from_json(data):
    okt = Okt()  # KoNLPy의 Okt 형태소 분석기 생성
    result = []

    for item in data:
        # Title 필드에서 텍스트를 가져와 명사 추출
        title = item.get("Title", "")
        tags = item.get("Tags", [])

        # 제목에서 명사 추출
        title_nouns = okt.nouns(title)

        # Title에서 추출한 명사와 Tags 병합 (중복 제거 위해 set 사용)
        combined_nouns = list(set(title_nouns + tags))

        # "ExtractedNouns" 필드로 추가하여 결과에 저장
        item["ExtractedNouns"] = combined_nouns
        result.append(item)

    return result

if __name__ == "__main__":
    # input.json 파일에서 데이터 읽기
    with open('input.json', 'r', encoding='utf-8') as file:
        data = json.load(file)

    # 명사 추출 함수 호출
    updated_data = extract_nouns_from_json(data)

    # 결과를 output.json 파일에 저장
    with open('output.json', 'w', encoding='utf-8') as file:
        json.dump(updated_data, file, ensure_ascii=False, indent=4)
