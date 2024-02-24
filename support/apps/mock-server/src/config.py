from typing import Any, Dict, List, Optional, Union
from os import environ, path

from pydantic import PostgresDsn, validator, AnyUrl
from pydantic_settings import BaseSettings

PORT = 8000
BIND = "0.0.0.0"
WORKERS = 2
RELOAD = True
