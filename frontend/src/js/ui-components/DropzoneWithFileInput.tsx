import styled from "@emotion/styled";
import {
  useRef,
  ReactNode,
  Ref,
  forwardRef,
  ReactElement,
  useState,
} from "react";
import { DropTargetMonitor } from "react-dnd";
import { NativeTypes } from "react-dnd-html5-backend";
import { useTranslation } from "react-i18next";

import { SelectFileButton } from "../button/SelectFileButton";
import FaIcon from "../icon/FaIcon";

import Dropzone, { ChildArgs, PossibleDroppableObject } from "./Dropzone";
import { ImportModal } from "./ImportModal";

export interface DragItemFile {
  type: "__NATIVE_FILE__"; // Actually, this seems to not be passed by react-dnd
  files: File[];
}

const FileInput = styled("input")`
  display: none;
`;

const SxDropzone = styled(Dropzone)<{ isInitial?: boolean }>`
  cursor: ${({ isInitial }) => (isInitial ? "initial" : "pointer")};
  transition: box-shadow ${({ theme }) => theme.transitionTime};
  position: relative;

  &:hover {
    box-shadow: 0 0 5px 0 rgba(0, 0, 0, 0.2);
  }
`;

const SxSelectFileButton = styled(SelectFileButton)`
  position: absolute;
  top: 3px;
  right: 0;
`;

const SxFaIcon = styled(FaIcon)`
  height: 10px;
  padding-right: 3px;
`;

interface PropsT<DroppableObject> {
  children: (args: ChildArgs<DroppableObject>) => ReactNode;
  onSelectFile?: (file: File) => void;
  onDrop: (
    item: DroppableObject | DragItemFile,
    monitor: DropTargetMonitor,
  ) => void;
  acceptedDropTypes?: string[];
  accept?: string;
  disableClick?: boolean;
  showImportButton?: boolean;
  isInitial?: boolean;
  className?: string;
  onImportLines?: (lines: string[]) => void;
}

/*
  Augments a dropzone with file drop support

  - opens file dialog on dropzone click
  - adds NativeTypes.FILE

  => The "onDrop"-prop needs to handle the file drop itself, though!
*/
const DropzoneWithFileInput = <
  DroppableObject extends PossibleDroppableObject = DragItemFile,
>(
  {
    onSelectFile,
    onImportLines,
    acceptedDropTypes,
    disableClick,
    showImportButton,
    children,
    onDrop,
    isInitial,
    className,
    accept,
  }: PropsT<DroppableObject>,
  ref: Ref<HTMLDivElement>,
) => {
  const { t } = useTranslation();
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  const dropTypes = [...(acceptedDropTypes || []), NativeTypes.FILE];

  const [importModalOpen, setImportModalOpen] = useState(false);

  function onSubmitImport(lines: string[]) {
    onImportLines?.(lines);
  }

  function onOpenFileDialog() {
    if (fileInputRef.current) {
      fileInputRef.current.click();
    }
  }

  return (
    <SxDropzone /* <FC<DropzoneProps<DroppableObject | DragItemFile>>> */
      acceptedDropTypes={dropTypes}
      onClick={() => {
        if (disableClick) return;

        if (onImportLines) {
          setImportModalOpen(true);
        } else {
          onOpenFileDialog();
        }
      }}
      onDrop={(item, monitor) => {
        if ("files" in item) {
          // Because it doesn't seem to be added by react-dnd
          item.type = NativeTypes.FILE;
        }

        onDrop(item as DroppableObject | DragItemFile, monitor);
      }}
      isInitial={isInitial}
      className={className}
      ref={ref}
    >
      {(args) => (
        <>
          {importModalOpen && (
            <ImportModal
              onClose={() => setImportModalOpen(false)}
              onSubmit={onSubmitImport}
            />
          )}
          {showImportButton && onImportLines && (
            <SxSelectFileButton onClick={() => setImportModalOpen(true)}>
              <SxFaIcon icon="file-import" gray />
              {t("common.import")}
            </SxSelectFileButton>
          )}
          {onSelectFile && (
            <FileInput
              ref={fileInputRef}
              type="file"
              accept={accept}
              onChange={(e) => {
                if (e.target.files) {
                  onSelectFile(e.target.files[0]);
                }

                if (fileInputRef.current) {
                  fileInputRef.current.value = "";
                }
              }}
            />
          )}
          {children(args as ChildArgs<DroppableObject>)}
        </>
      )}
    </SxDropzone>
  );
};

export default forwardRef(DropzoneWithFileInput) as <
  DroppableObject extends PossibleDroppableObject = DragItemFile,
>(
  p: PropsT<DroppableObject> & { ref?: Ref<HTMLDivElement> },
) => ReactElement;
