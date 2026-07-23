import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const sourcePath = "/Users/xiongxiao/Desktop/brew-services-inventory-20260721-streampark-dolphinscheduler-ui.xlsx";
const input = await FileBlob.load(sourcePath);
const workbook = await SpreadsheetFile.importXlsx(input);

const summary = await workbook.inspect({
  kind: "workbook,sheet,table,region",
  maxChars: 12000,
  tableMaxRows: 12,
  tableMaxCols: 12,
  tableMaxCellChars: 120,
});

console.log(summary.ndjson);
