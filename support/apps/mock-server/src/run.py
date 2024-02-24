import uvicorn
from config import PORT, BIND, WORKERS, RELOAD

if __name__ == "__main__":
    uvicorn.run(
        "main:app", host=BIND, port=int(PORT), reload=RELOAD, workers=int(WORKERS)
    )
