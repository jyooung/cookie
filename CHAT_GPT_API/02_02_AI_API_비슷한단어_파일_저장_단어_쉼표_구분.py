# 아래 경로에 A110.txt 파일을 생성해서 한글로 "추석"과 동일하게 불리우는 단어를 10개 저장하는 파이썬 코드를 작성해줄래 ?
# 단어와 단어 사이는 쉼표로 구분하여 파일에 저장되도록 코드를 작성해줘
# 경로명 : C:\Users\ASUS\Desktop\CHAT_GPT_API

# 저장할 단어 리스트
chuseok_synonyms = [
    "한가위", "가배", "중추절", "송편절", "큰명절", 
    "국풍절", "조상제", "보름날", "설날", "천고마비의 계절"
]

# 파일 경로 설정
file_path = r"C:\Users\ASUS\Desktop\CHAT_GPT_API\A110.txt"

# 파일에 단어 저장
with open(file_path, 'w', encoding='utf-8') as file:
    # 단어들을 쉼표로 구분하여 저장
    file.write(", ".join(chuseok_synonyms))

print("파일이 성공적으로 저장되었습니다.")

