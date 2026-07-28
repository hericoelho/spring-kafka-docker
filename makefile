export ROOT_DIR := $(shell git rev-parse --show-toplevel)
COMPOSE := $(shell command -v podman >/dev/null && echo "podman compose" || echo "docker compose")

exec:
	cd $(ROOT_DIR)/kafka && $(COMPOSE) up -d
	cd $(ROOT_DIR)/springQueue && $(COMPOSE) up

down:
	cd $(ROOT_DIR)/springQueue && $(COMPOSE) down
	cd $(ROOT_DIR)/kafka && $(COMPOSE) down