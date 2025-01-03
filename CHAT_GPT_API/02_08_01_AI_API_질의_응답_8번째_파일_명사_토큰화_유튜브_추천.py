# 안녕, 핵심 키워드 10개를 쉼표로 구분하여 C100.txt 파일에 저장하는 파이썬 코드를 작성하려고 합니다
# 서울의 가을 정취를 느낄 수 있는 영상을 검색할 핵심 키워드 10개를 쉼표로 구분하여 C100.txt 파일에 저장해줄래 ?
# 파일의 경로 : C:\Users\ASUS\Desktop\CHAT_GPT_API 

# 기본 API 연동 파이썬 코드
import openai

# API 키 설정
openai.api_key = "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"  # 여기서 "YOUR_OPENAI_API_KEY"를 본인의 실제 API 키로 교체하세요.

# ChatGPT 모델을 사용하려면 v1/chat/completions 엔드포인트 사용
response = openai.ChatCompletion.create(
    model="gpt-3.5-turbo",  # 사용하고 싶은 모델명
    messages=[
        {"role": "system", "content": "You are a helpful assistant."},
        {"role": "user", "content": "서울의 가을 정취를 느낄 수 있는 영상을 검색할 핵심 키워드 10개를 알려줄래 ?"}
    ]
)

print(response['choices'][0]['message']['content'])