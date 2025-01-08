#!/bin/bash

# Шаг 1: Сборка проекта
echo "Собираем проект..."
mvn clean package -DskipTests || { echo "Ошибка сборки. Прерываем скрипт."; exit 1; }

# Проверка наличия файла
if [ ! -f target/LOKIVPN-0.0.1-SNAPSHOT.jar ]; then
    echo "Файл сборки не найден. Прерываем скрипт."
    exit 1
fi

# Шаг 2: Отправка файла на сервер
echo "Отправляем сборку на сервер..."
scp target/LOKIVPN-0.0.1-SNAPSHOT.jar root@5.35.92.4:/opt/lokivpn || { echo "Ошибка отправки файла. Прерываем скрипт."; exit 1; }

# Шаг 3: Перезапуск сервиса на сервере через systemd
echo "Перезапускаем бот на сервере..."
ssh root@5.35.92.4 "
  if [ -f /opt/lokivpn/LOKIVPN-0.0.1-SNAPSHOT.jar ]; then
    mv /opt/lokivpn/LOKIVPN-0.0.1-SNAPSHOT.jar /opt/lokivpn/LOKIVPN-0.0.1-SNAPSHOT.jar.bak;
  fi
  mv /opt/lokivpn/LOKIVPN-0.0.1-SNAPSHOT.jar.bak /opt/lokivpn/LOKIVPN-0.0.1-SNAPSHOT.jar;
  systemctl restart lokivpn-bot.service
" || { echo "Ошибка при перезапуске сервиса. Прерываем скрипт."; exit 1; }

echo "Готово!"
