# 최초 네트워크 접속 질의 응답

# 아래의 요청은 B100.txt 파일에 저장된 질문내용을 문자열 T100 변수로 저장하여 인공지능에게 질문으로 던지는 형식으로 코드를 수정하기 위함 입니다
# 아래의 코드는 챗GPT 에게 질문하는 파이썬 코드 입니다
# 아래의 질문을 B100.txt 파일에 저장하였습니다
# 파일의 경로 : C:\Users\ASUS\Desktop\CHAT_GPT_API
# 아래의 코드를 수정하여 B100.txt 파일을 질문으로 던지는 형식으로 코드를 수정하려고 합니다
# 이를 위하여 B100.txt 파일을 오픈하여 파일 내부의 txt 를 문자열 T100 변수로 저장하는 코드를 추가 합니다
# 그리고 문자열 T100 변수를 질문으로 던지는 형식으로 코드를 수정해줄래 ?

# import openai

# # API 키 설정
# openai.api_key = "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"  # 여기서 "YOUR_OPENAI_API_KEY"를 본인의 실제 API 키로 교체하세요.

# # ChatGPT 모델을 사용하려면 v1/chat/completions 엔드포인트 사용
# response = openai.ChatCompletion.create(
#     model="gpt-3.5-turbo",  # 사용하고 싶은 모델명
#     messages=[
#         {"role": "system", "content": "You are a helpful assistant."},
#         {"role": "user", "content": "포도라는 단어를 영어로 알려줄래 ?"}
#     ]
# )

# print(response['choices'][0]['message']['content'])


# # 아래의 요청은 B100.txt 파일에 저장된 질문내용을 문자열 T100 변수로 저장하여 인공지능에게 질문으로 던지는 코드 입니다
# # B200.txt 파일의 질문내용을 문자열 T200 변수로 저장하여 인공지능에게 T100, T200 을 질문으로 던지고 T100 과 T200 답변을 모두 받을 수 있도록 하는 코드로 아래의 소스코드를 수정해줄래 ?

# import openai

# # API 키 설정
# openai.api_key = "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"  # 실제 API 키로 교체하세요.

# # B100.txt 파일에서 질문 내용을 읽어오는 부분
# file_path = r"C:\Users\ASUS\Desktop\CHAT_GPT_API\B100.txt"

# with open(file_path, 'r', encoding='utf-8') as file:
#     T100 = file.read()  # 파일의 내용을 T100 변수에 저장

# # ChatGPT 모델에 질문을 보내는 코드
# response = openai.ChatCompletion.create(
#     model="gpt-3.5-turbo",  # 사용하고 싶은 모델명
#     messages=[
#         {"role": "system", "content": "You are a helpful assistant."},
#         {"role": "user", "content": T100}  # 파일에서 읽은 내용을 질문으로 사용
#     ]
# )

# # 응답 출력
# print(response['choices'][0]['message']['content'])

import openai

# API 키 설정
openai.api_key = "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"  # 실제 API 키로 교체하세요.

# 파일 경로 설정
file_path_B100 = r"C:\Users\LG\Desktop\CHAT_GPT_API\B100.txt"
file_path_B200 = r"C:\Users\LG\Desktop\CHAT_GPT_API\B200.txt"

# B100.txt 파일에서 질문 내용을 읽어와 T100 변수에 저장
with open(file_path_B100, 'r', encoding='utf-8') as file:
    T100 = file.read()

# B200.txt 파일에서 질문 내용을 읽어와 T200 변수에 저장
with open(file_path_B200, 'r', encoding='utf-8') as file:
    T200 = file.read()

# ChatGPT 모델에 두 개의 질문을 동시에 보내는 코드
response = openai.ChatCompletion.create(
    model="gpt-3.5-turbo",  # 사용하고 싶은 모델명
    messages=[
        {"role": "system", "content": "You are a helpful assistant."},
        {"role": "user", "content": T100},  # B100.txt 내용
        {"role": "user", "content": T200}   # B200.txt 내용
    ]
)

# 응답 출력
for i, choice in enumerate(response['choices']):
    print(f"Response to T{i + 1}00:\n{choice['message']['content']}\n")
