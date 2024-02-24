from pydantic import BaseModel, Field
from typing import Generic, ParamSpec, TypeVar
from prometheus_fastapi_instrumentator import Instrumentator
from fastapi import FastAPI, HTTPException
import asyncio
import random

class Credit(BaseModel):
    amount: int

class Debit(BaseModel):
    amount: int

class State(BaseModel):
    credits: list[Credit]
    debits: list[Debit]

class Transfer(BaseModel):
    amount: int
    to: int

class Response(BaseModel):
    msg: str

app = FastAPI()

Instrumentator().instrument(app).expose(app
                                        # , include_in_schema=False, should_gzip=True
                                        )

state = State(credits=[Credit(amount=201)], debits=[Debit(amount=1)])

@app.get("/api/v1/state", response_model=State)
async def get_state() -> State:
    global state
    return state

@app.post("/api/v1/state", response_model=State)
async def set_state(st: State) -> State:
    global state
    state = st
    return state

@app.post("/payments/transfer", response_model=Response)
async def receive_transfer(transfer: Transfer) -> Response:

    await asyncio.sleep(int(random.random() * 2000) * 0.001)

    if transfer.to in [3, 5]:
        raise HTTPException(status_code=403, detail="Transfer rejected")
    else:
        return Response(msg="Transfer received")
