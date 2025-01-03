# pip install openai

# python.exe -m pip install --upgrade pip

# openai 라이브러리 임포트
# import openai

# # OpenAI API 키 설정
# openai.api_key = "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"  # 여기서 "YOUR_OPENAI_API_KEY"를 본인의 실제 API 키로 교체하세요.

# # 텍스트 생성 요청 (gpt-3.5-turbo 또는 gpt-4 모델로 변경)
# response = openai.Completion.create(
#     model="gpt-3.5-turbo",  # 사용 가능한 최신 모델
#     prompt="Hello, how are you today?",  # GPT에게 줄 질문 또는 프롬프트
#     max_tokens=100  # 생성할 최대 토큰 수
# )

# # 생성된 텍스트 출력
# print(response.choices[0].text.strip())


# ##############################

import openai

# API 키 설정
openai.api_key = "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"  # 여기서 "YOUR_OPENAI_API_KEY"를 본인의 실제 API 키로 교체하세요.

# ChatGPT 모델을 사용하려면 v1/chat/completions 엔드포인트 사용
response = openai.ChatCompletion.create(
    model="gpt-3.5-turbo",  # 사용하고 싶은 모델명
    messages=[
        {"role": "system", "content": "You are a helpful assistant."},
        {"role": "user", "content": "Hello, how are you?"}
    ]
)

print(response['choices'][0]['message']['content'])

