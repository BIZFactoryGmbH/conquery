import { t } from "i18next";
import {
  DateStatistics,
  NumberStatistics,
  PreviewStatistics,
  StringStatistics,
} from "../api/types";
import { parseDate } from "../common/helpers/dateHelper";

const DIGITS_OF_PRECISION = 3;
export function formatNumber(num: number): string {
  return num.toLocaleString();
  // TODO verify localeString implementation
  if (num > 100) {
    return numberToThreeDigitArray(Math.floor(num)).join(".")
  }
  return t("preview.dateError");
}

export function previewStatsIsStringStats(
  stats: PreviewStatistics,
): stats is StringStatistics {
  return stats.chart === "HISTO";
}

export function previewStatsIsNumberStats(
  stats: PreviewStatistics,
): stats is NumberStatistics {
  return stats.chart === "DESCRIPTIVE";
}

export function previewStatsIsDateStats(
  stats: PreviewStatistics,
): stats is DateStatistics {
  return stats.chart === "DATES";
}
