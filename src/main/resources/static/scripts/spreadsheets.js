function copySpreadSheet() {
    document.getElementById("resultData").innerHTML = "";
    fetch("http://localhost:8080/spreadsheet/copy", {
      method: "POST",
      body: JSON.stringify({
        sourceSpreadSheetId: document.getElementById("sourceSpreadSheetId").value,
        resultSpreadSheetId: document.getElementById("resultSpreadSheetId").value,
        range: document.getElementById("range").value,
      }),
      headers: {
        "Content-type": "application/json; charset=UTF-8"
      }
    })
      .then(response => response.json())
      .then(json => {
        console.log(json)
        document.getElementById("resultData").innerHTML = "result spreadsheetId: " + json.spreadsheetId + "<br/>"
        + "range: " + json.tableRange + "<br/>"
        + "<a href=\"https://docs.google.com/spreadsheets/d/" + json.spreadsheetId + "/edit#gid=0\" target=\"_blank\">Updated document" + "</a>"
       });
}