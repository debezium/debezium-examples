import os

from flask import Flask, render_template_string, request
from google.api_core.client_options import ClientOptions
from google.auth.credentials import AnonymousCredentials
from google.cloud import bigquery

PROJECT = os.getenv("BIGQUERY_PROJECT", "my-project")
ENDPOINT = os.getenv("BIGQUERY_ENDPOINT", "http://localhost:9050")
DEFAULT_QUERY = "SELECT * FROM my-dataset.customers ORDER BY write_time DESC"

client = bigquery.Client(
    PROJECT,
    client_options=ClientOptions(api_endpoint=ENDPOINT),
    credentials=AnonymousCredentials(),
)

app = Flask(__name__)

PAGE = """<!doctype html>
<title>BigQuery emulator</title>
<h1>BigQuery emulator: {{ project }}</h1>
<p>Tables:
{% for table in tables %}
  <a href="?query={{ ('SELECT * FROM ' ~ table) | urlencode }}">{{ table }}</a>
{% endfor %}
</p>
<form>
  <textarea name="query" rows="4" cols="100">{{ query }}</textarea><br>
  <button>Run</button>
</form>
{% if error %}
  <pre>{{ error }}</pre>
{% else %}
  <p>{{ rows | length }} rows</p>
  <table border="1">
    <tr>{% for column in columns %}<th>{{ column }}</th>{% endfor %}</tr>
    {% for row in rows %}
      <tr>{% for value in row %}<td>{{ value }}</td>{% endfor %}</tr>
    {% endfor %}
  </table>
{% endif %}
"""


@app.route("/")
def index():
    query = request.args.get("query", DEFAULT_QUERY)
    tables = [
        f"{table.dataset_id}.{table.table_id}"
        for dataset in client.list_datasets()
        for table in client.list_tables(dataset)
    ]
    columns, rows, error = [], [], None
    try:
        result = client.query_and_wait(query, job_retry=None)
        columns = [field.name for field in result.schema]
        rows = [list(row.values()) for row in result]
    except Exception as e:
        error = str(e)
    return render_template_string(
        PAGE, project=PROJECT, tables=tables, query=query, columns=columns, rows=rows, error=error
    )


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8000)
