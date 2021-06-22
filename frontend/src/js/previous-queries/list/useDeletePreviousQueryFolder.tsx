import { StateT } from "app-types";
import { useTranslation } from "react-i18next";
import { useSelector, useDispatch } from "react-redux";

import { usePatchStoredQuery } from "../../api/api";
import type { DatasetIdT } from "../../api/types";
import { setMessage } from "../../snack-message/actions";

import { useLoadPreviousQueries } from "./actions";
import type { PreviousQueryT } from "./reducer";

export const useDeletePreviousQueryFolder = (
  folder: string,
  onSuccess?: () => void,
) => {
  const { t } = useTranslation();
  const datasetId = useSelector<StateT, DatasetIdT | null>(
    (state) => state.datasets.selectedDatasetId,
  );
  const dispatch = useDispatch();
  const patchStoredQuery = usePatchStoredQuery();
  const loadPreviousQueries = useLoadPreviousQueries();

  const queries = useSelector<StateT, PreviousQueryT[]>(
    (state) => state.previousQueries.queries,
  );

  return async () => {
    if (!datasetId) return;

    try {
      await Promise.all(
        queries
          .filter((query) => query.tags.includes(folder))
          .map((query) => {
            const nextTags = query.tags.filter((tag) => tag !== folder);

            return patchStoredQuery(datasetId, query.id, { tags: nextTags });
          }),
      );

      await loadPreviousQueries(datasetId);

      if (onSuccess) {
        onSuccess();
      }
    } catch (e) {
      dispatch(setMessage({ message: t("previousQuery.retagError") }));
    }
  };
};
