# 최초 네트워크 접속 질의 응답

import openai

# API 키 설정
openai.api_key = "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"  # 여기서 "YOUR_OPENAI_API_KEY"를 본인의 실제 API 키로 교체하세요.

# ChatGPT 모델을 사용하려면 v1/chat/completions 엔드포인트 사용
response = openai.ChatCompletion.create(
    model="gpt-3.5-turbo",  # 사용하고 싶은 모델명
    messages=[
        {"role": "system", "content": "You are a helpful assistant."},
        {"role": "user", "content": "포도라는 단어를 영어로 알려줄래 ?"}
    ]
)

print(response['choices'][0]['message']['content'])

