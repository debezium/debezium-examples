ARG TENSORFLOW_VERSION
FROM tensorflow/tensorflow:${TENSORFLOW_VERSION}-jupyter

COPY requirements.txt /tf
RUN python3 -m pip install --upgrade pip && python3 -m pip install -r requirements.txt