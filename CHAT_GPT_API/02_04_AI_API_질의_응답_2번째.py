# 최초 네트워크 접속 질의 응답

# 아래의 경로에 A110.txt 파일이 있습니다
# 이 파일에는 단어가 10개 저장되어 있습니다
# 경로명 : C:\Users\ASUS\Desktop\CHAT_GPT_API
# 이 파일에 "설날"이라는 단어가 있나요 ?

# 아래의 경로에 A110.txt 파일이 있습니다. 이 파일에는 단어가 10개 저장되어 있습니다. 경로명 : C:\Users\ASUS\Desktop\CHAT_GPT_API 이 파일에 "설날"이라는 단어가 있나요 ?

import openai

# API 키 설정
openai.api_key = "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"  # 여기서 "YOUR_OPENAI_API_KEY"를 본인의 실제 API 키로 교체하세요.

# ChatGPT 모델을 사용하려면 v1/chat/completions 엔드포인트 사용
response = openai.ChatCompletion.create(
    model="gpt-3.5-turbo",  # 사용하고 싶은 모델명
    messages=[
        {"role": "system", "content": "You are a helpful assistant."},
        {"role": "user", "content": """아래의 경로에 A110.txt 파일이 있습니다. 이 파일에는 단어가 10개 저장되어 있습니다. 
경로명 : C:\\Users\\LG\\Desktop\\CHAT_GPT_API
이 파일에 "설날"이라는 단어가 있나요?"""}
    ]
)

print(response['choices'][0]['message']['content'])

