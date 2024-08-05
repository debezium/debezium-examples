FROM python:3.11-slim

RUN apt update && apt install -y git
RUN git clone https://github.com/googleapis/python-pubsub.git

WORKDIR python-pubsub/samples/snippets
RUN pip install -r requirements.txt
