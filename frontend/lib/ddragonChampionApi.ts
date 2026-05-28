const DDRAGON_VERSION =
  process.env.NEXT_PUBLIC_DDRAGON_VERSION ?? "15.24.1";

export type ChampionNameMap = Record<string, string>;

type DataDragonChampion = {
  id: string;
  key: string;
  name: string;
};

type DataDragonChampionResponse = {
  data: Record<string, DataDragonChampion>;
};

export async function fetchKoreanChampionNameMap(): Promise<ChampionNameMap> {
  const response = await fetch(
    `https://ddragon.leagueoflegends.com/cdn/${DDRAGON_VERSION}/data/ko_KR/champion.json`,
    {
      method: "GET",
      cache: "force-cache",
    }
  );

  if (!response.ok) {
    throw new Error("한글 챔피언 이름 데이터를 불러오지 못했습니다.");
  }

  const json = (await response.json()) as DataDragonChampionResponse;

  const nameMap: ChampionNameMap = {};

  Object.values(json.data).forEach((champion) => {
    nameMap[champion.id] = champion.name;
  });

  return nameMap;
}

export function getKoreanChampionName(
  championIdName: string,
  nameMap?: ChampionNameMap
) {
  if (!championIdName) {
    return "";
  }

  if (!nameMap) {
    return championIdName;
  }

  return nameMap[championIdName] ?? championIdName;
}