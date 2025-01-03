

# 저장할 단어 리스트
chuseok_synonyms = [
    "한가위", "가배", "중추절", "송편절", "큰명절", 
    "국풍절", "조상제", "보름날", "설날", "천고마비의 계절"
]

# 파일 경로 설정
file_path = r"C:\Users\ASUS\Desktop\CHAT_GPT_API\A100.txt"

# 파일에 단어 저장
with open(file_path, 'w', encoding='utf-8') as file:
    for word in chuseok_synonyms:
        file.write(word + "\n")

print("파일이 성공적으로 저장되었습니다.")
