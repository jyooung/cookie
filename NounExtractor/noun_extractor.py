import json
import os
from konlpy.tag import Okt

def extract_nouns_from_json(data):
    okt = Okt()
    result = []

    for item in data:
        title = item.get("Title", "")
        tags = item.get("Tags", [])
        title_nouns = okt.nouns(title)
        combined_nouns = list(set(title_nouns + tags))
        item["ExtractedNouns"] = combined_nouns
        result.append(item)

    return result

def process_video_data(input_file_path, output_file_path):
    if not os.path.exists(input_file_path):
        print(f"Error: {input_file_path} 파일이 존재하지 않습니다.")
        return

    with open(input_file_path, 'r', encoding='utf-8') as file:
        data = json.load(file)

    if not data:  # 데이터가 비어있는 경우
        print("입력 데이터가 비어 있습니다. output.json을 빈 배열로 생성합니다.")
        updated_data = []  # 빈 데이터 처리
    else:
        updated_data = extract_nouns_from_json(data)

    # output.json 파일 생성 및 데이터 저장
    with open(output_file_path, 'w', encoding='utf-8') as file:
        json.dump(updated_data, file, ensure_ascii=False, indent=4)
    print(f"Data successfully saved to {output_file_path}")

if __name__ == "__main__":
    input_file_path = "C:/SpringProject/personalCookie/btnPJ/input.json"  # 절대 경로 사용
    output_file_path = "C:/SpringProject/personalCookie/btnPJ/output.json"  # 절대 경로 사용
    process_video_data(input_file_path, output_file_path)
