# HighLoad Cup 2017

Решение, занявшее в первом раунде [чемпионата](https://highloadcup.ru) 9 место.

Место в финале пока неизвестно.

Написано на **Java 8** с использованием модуля на **C** через **JNI** для работы с **epoll**.

## Основные идеи
* Использование C для осуществления сетевых операций напрямую через syscall в ядро linux;
* Минимизация аллокаций новых объектов;
* Преаллокация буфферов heap и native памяти в пулах;
* Минимизация копирования памяти между native и heap;
* Кэширование ответов на все типы GET-запросов (в том числе с фильтрами по /avg, /visits);
* Очистка только тех кэшей, которые аффектит изменение аттрибутов данного объекта;
* Отказ от использования String в пользу байтовых массивов;
* Отказ от использования Map в пользу Array;
* Реимплементация стандатных функций Java под конкретную задачу;
* Собственный HTTP-парзер;
* Подготовка готовых ответов для стандартных ситуаций (404, 400, 200 empty);
* Минимизация переключений контекстов, минимизация общих данных между потоками;
* Оптимизация генерации и парсинга Json.

## Внимание!

Код решения написан под конкретную задачу конкурса и крайне не рекомендуется к использованию где-то ещё без значительной доработки. В реальной разработке не стоит использовать такое количество велосипедов и оптимизаций.