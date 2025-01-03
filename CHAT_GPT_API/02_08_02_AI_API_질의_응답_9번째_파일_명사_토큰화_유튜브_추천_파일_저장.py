# 안녕, 핵심 키워드 10개를 쉼표로 구분하여 C100.txt 파일에 저장하는 파이썬 코드를 작성하려고 합니다
# 서울의 가을 정취를 느낄 수 있는 영상을 검색할 핵심 키워드 10개를 쉼표로 구분하여 C100.txt 파일에 저장해줄래 ?
# 파일의 경로 : C:\Users\ASUS\Desktop\CHAT_GPT_API 

# # 인공지능 API 를 통한 답변을 파일에 저장하는 파이썬 코드를 작성하려고 합니다. 자세한 내용은 다음과 같습니다
# # 아래의 기본 API 연동 파이썬 코드는 인공지능에게 질의 응답을 하는 코드 입니다
# # 화면으로 출력되는 인공지능의 답변을 파일에 저장하도록 하려고 합니다
# # 아래의 코드를 수정하여 C100.txt 파일에 인공지능의 답변을 저장하도록 파이썬 코드를 작성해줄래 ?
# # 파일에 저장시에는 키워드간에 쉼표로 구분하여 저장될 수 있도록 합니다
# # 파일의 경로 : C:\Users\ASUS\Desktop\CHAT_GPT_API

# # 기본 API 연동 파이썬 코드
# import openai

# # API 키 설정
# openai.api_key = "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"  # 여기서 "YOUR_OPENAI_API_KEY"를 본인의 실제 API 키로 교체하세요.

# # ChatGPT 모델을 사용하려면 v1/chat/completions 엔드포인트 사용
# response = openai.ChatCompletion.create(
#     model="gpt-3.5-turbo",  # 사용하고 싶은 모델명
#     messages=[
#         {"role": "system", "content": "You are a helpful assistant."},
#         {"role": "user", "content": "서울의 가을 정취를 느낄 수 있는 영상을 검색할 핵심 키워드 10개를 알려줄래 ?"}
#     ]
# )

# print(response['choices'][0]['message']['content'])

import openai

# API 키 설정
openai.api_key = "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"  # 본인의 실제 API 키로 교체하세요.

# ChatGPT 모델을 사용하여 응답 생성
response = openai.ChatCompletion.create(
    model="gpt-3.5-turbo",  # 사용하고 싶은 모델명
    messages=[
        {"role": "system", "content": "You are a helpful assistant."},
        {"role": "user", "content": "서울의 가을 정취를 느낄 수 있는 영상을 검색할 핵심 키워드 10개를 알려줄래 ?"}
    ]
)

# 인공지능의 답변 가져오기
answer = response['choices'][0]['message']['content']

# 답변을 쉼표로 구분하여 저장
keywords = answer.replace('\n', ', ')  # 줄바꿈을 쉼표로 대체하여 저장

# 파일에 저장
file_path = r"C:\Users\LG\Desktop\CHAT_GPT_API\C100.txt"
with open(file_path, "w", encoding="utf-8") as file:
    file.write(keywords)

print("답변이 C100.txt 파일에 저장되었습니다.")
