## Сервис заказов товаров онлайн.

![png](docs/README.assets/usecase.png)

### Пользовательские истории
<details>
<summary>Click to expand</summary>

* Регистрация пользователя
  * Как **незарегистрированный пользователь** я хочу иметь возможность **зарегистрироваться** в приложении, чтобы иметь возможность пользоваться им.

* Аутентификация
  * Как **зарегистрированный пользователь** я хочу иметь возможность **войти** в приложение, предоставляя свои **логин** и **пароль** указанные при регистрации, чтобы начать пользоваться функциональными возможностями приложения.

* Выход из приложения
  * Как **зарегистрированный пользователь** я хочу иметь возможность **выйти** из приложения, чтобы воспользоваться другим **аккаунтом**.

* Пополнение внутреннего счета
  * Как **зарегистрированный пользователь** я хочу иметь возможность пополнить свой **счет**, чтобы иметь возможность совершать **покупки**.
  * Как **зарегистрированный пользователь** я хочу иметь возможность узнать количество средств на моем **счету**.

* Поиск товара
  * Как **зарегистрированный пользователь** я хочу иметь возможность **найти** интересующие меня **товары**, чтобы узнать о наличии, цене и иметь возможность совершить покупку.
  * Информация о товаре включает в себя
    * Заголовок
    * Описание
    * Доступное количество товара
    * Стоимость единицы товара

* Покупка товара
  * Как **зарегистрированный пользователь** я хочу иметь возможность **купить** **товары**, потому что я хочу им обладать.

* История заказов
  * Как **зарегистрированный пользователь** я хочу иметь возможность просматривать историю своих **заказов**.

* Пополнение ассортимента
  * Как **администратор** я должен иметь возможность пополнять ассортимент **товаров**, чтобы в будущем их продать.
  
</details>

## Инфраструктура

* Все компоненты приложения упакованы в helm чарты или yaml манифесты, готовые для развертывания в k8s кластере.
* Все обращения к сервисам из вне происходит через API Gateway (Nginx).
* Используется pull-based метрики (prometheus + визуализация grafana).
* Сервисы представляют REST API.

## Регистрация и аутентификация

![png](docs/README.assets/auth.png)

## Размещение заказа

Для осуществления распределенной транзакции используются реализация паттерна `Orchestration-based saga`. 
В качестве оркестратора используется [Temporal.io](https://temporal.io/).

![mermaid-diagram-20200526103254](docs/README.assets/saga.png)

Таблица транзакций

| Step  | Service     | Transaction      | Compensation transaction          |
| :---  |    :----:   |   :----:         |                     :----:          |
| 1     | Order service       | POST /orders/ | PUT /orders/declines/{orderId}    | 
| 2     | Inventory service        | POST /items/reservations         | POST /items/reservations/cancellations/{reservationId} |
| 3     | Delivery service         | POST /deliveries | POST /deliveries/{deliveryId}/cancellation |
| 4     | Billing service          | POST /payments/credits | --- |
| 5     | Order service            | PUT /orders/approvals  | --- |

Транзакция №4 является поворотной и не имеет компенсирующей транзакции.

```kotlin
override fun start(event: CreatedOrderEvent) {
        val sagaOptions = Saga.Options.Builder().setParallelCompensation(true).build()
        val saga = Saga(sagaOptions)
        try {
            activities.placeOrder()
            saga.addCompensation(activities::declineOrder, event.orderId)

            val reservationId = activities.reserveItems(event.consumerId, event.orderId, event.items)
            saga.addCompensation(activities::cancelReservation, reservationId)

            val deliveryId = activities.reserveDelivery(event.orderId, event.delivery)
            saga.addCompensation(activities::cancelDelivery, deliveryId)

            val paymentId = activities.makePayment(event.consumerId, event.orderId, event.items)
            activities.confirmOrder(event.orderId, deliveryId, paymentId, reservationId)
        } catch (exc: ActivityFailure) {
            saga.compensate()
            throw exc
        }
    }
```

## Установка

Установка `prometheus`
```
kubectl create ns monitoring
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm upgrade --install -n monitoring -f infra/prometheus/prometheus.yaml prometheus prometheus-community/kube-prometheus-stack --atomic
```

Установка `kafka`
```
kubectl create ns kafka
kubectl apply -f infra/kafka/pv.yaml -n kafka
helm upgrade --install -n kafka cp confluentinc/cp-helm-charts -f infra/kafka/cp_values.yaml
// Подождать пока все поды поднимутся ~5-6 минут
watch -n 5 kubectl get pods -n kafka
kubectl apply -f infra/kafka/debezium_connector.yaml -n kafka
```

Установка `temporal`
```
kubectl create ns temporalio
helm repo add bitnami https://charts.bitnami.com/bitnami
helm install -n temporalio temporal-storage -f infra/temporal-storage/values.yaml bitnami/postgresql --version=9.2.1
// Подождать пару минут пока поднимется хранилище для temporal io
watch -n 5 kubectl get pods -n temporalio
helm upgrade --install -n temporalio -f infra/temporal/values.yaml temporalio infra/temporal/.
```

Установка `order-service`
```
kubectl create ns order-service
helm upgrade --install -n order-service -f infra/order-service/values.yaml order-service infra/order-service/.
```

Установка `user-service`
```
kubectl create ns user-service
helm upgrade --install -n user-service -f infra/user-service/values.yaml user-service infra/user-service/.
```

Установка `billing-service`
```
kubectl create ns billing-service
helm upgrade --install -n billing-service -f infra/billing-service/values.yaml billing-service infra/billing-service/.
```

Установка `delivery-service`
```
kubectl create ns delivery-service
helm upgrade --install -n delivery-service -f infra/delivery-service/values.yaml delivery-service infra/delivery-service/.
```

Установка `inventory-service`
```
kubectl create ns inventory-service
helm upgrade --install -n inventory-service -f infra/inventory-service/values.yaml inventory-service infra/inventory-service/.
```

Установка `orchestrator`
```
kubectl create ns orchestrator
helm upgrade --install -n orchestrator -f infra/orchestrator/values.yaml orchestrator infra/orchestrator/.
```

Установка `api-gateway`
```
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm upgrade --install -f infra/prometheus/nginx-ingress.yaml nginx ingress-nginx/ingress-nginx --atomic
kubectl apply -f infra/api-gateway/ingress.yaml
```

Добавить коннектор
```
curl -X POST http://$(minikube ip):30500/connectors -H 'Content-Type: application/json' -d @debezium/connectors/prod/order-source-connector.json
curl -X POST http://$(minikube ip):30500/connectors -H 'Content-Type: application/json' -d @debezium/connectors/prod/user-source-connector.json
curl -X POST http://$(minikube ip):30500/connectors -H 'Content-Type: application/json' -d @debezium/connectors/prod/billing-sink-connector.json
```

## Тестирование

```
newman run --verbose integration_tests/saga_collections.json --delay-request 10000
```

## Описание сервисов

* User service
  * Ответственен за регистрацию/аутентификацию и хранение профилей пользователей

* Order service
  * Управляет агрегатом "Заказ", который предоставляет согласованное представление о заказе и его текущем статусе.
  * Сервис стартует сагу, создающую заказ.

* Delivery service:
  * Отвечает за резервирование доставки.
  * Ничего не знает о других сервисах.

* Inventory service:
  * Управляет данными о доступным товарах.
  * Позволяет зарезервировать товар.
  * Предоставляет функцию поиска доступных товаров.
  * Гарантирует, что не будет зарезервирован недоступный на момент заказа товар.
  * Вычисляет сумму резервирования.
  * Ничего не знает про другие сервисы.

* Billing service:
  * Упраляет данными о балансе пользователя.
  * Хранит и ведет историю финансовых транзакций (пополнение баланса и оплата).
  * Отвечает за консистентность финансовый данных.
  * Слушает события **User service**. Создает финансовый аккаунт при регистрации пользователя.

* Notification service:
  * Рассылает уведомления.

* API gateway
  * Ответственен за роутинг запросов
  * Rate limiting
  * Кэширование
  * Canary деплои

## Контракты

[Описание контрактов](http://petstore.swagger.io/?url=https://raw.githubusercontent.com/ivahotin/otus-microservice-project/master/docs/contracts/restapi.yaml)

## Мониторинг

Проброс порта к Grafana

`kubectl port-forward -n monitoring svc/prometheus-grafana 9000:80`

После этого Grafana будет доступна на `localhost:9000`.

Логин/Пароль для доступа `admin/prom-operator`

## Альтернативы

* Сервис **UserService** будет объединен с сервисом **AuthService**. В этом случае можно будет объединить операции регистрации и создание профиля. Для сессий будет использовано отдельное хранилище типа KV.
* Сервис **StorageService** обеспечивает только базовый поиск по префиксу имени товара. Для того чтобы обеспечить более продвинутый поиск можно добавить дополнительный сервис **SearchService**. Новый сервис может подписаться на события из сервиса **StorageService** и обновлять поисковый индекс.
* Можно было бы обойтись без оркестратора, реализовав распределенные транзакции через хореографию. Однако это повышает сложность каждого сервиса (они должны слушать события из брокера сообщений), помимо этого усложняется понимание системы и ее отладка. Я предпочел иметь единый оркестратор.
* Можно было бы реализовать авторизацию через jwt токены, чтобы избавиться от хранилища сессий. **ApiGateway** в таком случае проводил бы верификацию токена.