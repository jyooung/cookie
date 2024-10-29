import json

def process_video_data(file_path):
    video_list = []  # 비디오 데이터를 저장할 리스트 초기화

    # 파일을 열고 내용을 읽음
    with open(file_path, 'r', encoding='utf-8') as file:
        videos = file.read().strip().split('\n\n')  # 비디오 데이터를 빈 줄 두 개를 기준으로 구분

        for video in videos:
            if not video.strip():  # 빈 줄이거나 잘못된 데이터는 스킵
                continue

            data = {}
            for line in video.split('\n'):
                if ": " in line:
                    key, value = line.split(": ", 1)
                    data[key.strip()] = value.strip()
            video_list.append(data)  # 변환된 데이터를 리스트에 추가

    # 결과를 JSON 형식으로 반환
    return json.dumps(video_list, ensure_ascii=False)

if __name__ == "__main__":
    import sys
    file_path = sys.argv[1]  # 첫 번째 인자로 파일 경로를 받음
    result = process_video_data(file_path)
    print(result)  # 결과를 JSON 형식으로 출력
