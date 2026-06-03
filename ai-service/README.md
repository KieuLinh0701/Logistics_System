## Cài đặt

```bash
cd ai-service
python -m venv .venv
.venv\Scripts\activate

pip install -r requirements.txt

Chạy ứng dụng
uvicorn app.main:app --host 0.0.0.0 --port 8001 --reload

