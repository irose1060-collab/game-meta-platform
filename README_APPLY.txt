META GG 전적 카드 상세보기 접기/펼치기 패치

적용 위치:
- /home/ubuntu/game-meta-platform 에서 압축 해제

적용 명령:
cd ~/game-meta-platform
unzip -o ~/metagg_match_detail_toggle_patch.zip

반영 명령:
cd ~/game-meta-platform/frontend
printf "NEXT_PUBLIC_API_BASE_URL=https://metagg.cloud\n" > .env.production
rm -rf .next
npm run build
pm2 restart metagg-frontend --update-env
pm2 save

수정 내용:
1. 전적 검색 결과의 각 경기 카드를 기본적으로 요약형으로 표시
2. 승/패, 챔피언, 포지션, KDA, 주요 수치만 먼저 표시
3. 상세보기 버튼을 누르면 최종 아이템, 아이템 구매 흐름, 스킬 레벨업, 팀 오브젝트, 양 팀 참가자 목록이 펼쳐짐
4. 상세 접기 버튼으로 다시 접을 수 있음

확인 방법:
https://metagg.cloud 접속 → 전적 검색 → 경기 카드에서 상세보기 버튼 확인
