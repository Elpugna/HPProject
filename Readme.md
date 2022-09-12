## Akka HTTP/Streams Project

---
## Dataset

Dataset taken from Kaggle.com: [Amazon US Customer Reviews](https://www.kaggle.com/datasets/cynthiarempel/amazon-us-customer-reviews-dataset).

### Dataset Columns

Dataset contains 15 columns:
* marketplace
* customer_id
* review_id
* product_id
* product_parent
* product_title
* product_category
* star_rating
* helpful_votes
* total_votes
* vine
* verified_purchase
* review_headline
* review_body
* review_date

### Dataset Entities
From the Dataset, 3 entities were identified and modeled as such:
- Customer
- - id:**Long**
- - name:**String** (Excluded on Amazon's dataset, but usable for original data that might be generated)
- Product
- - id:**String**
- - title:**String**
- - category:**String**
- Review
- - id:**String**
- - title:**String**
- - rating:**Int (1-5)**
- - date: **YYYY/MM/DD**
- - body:**String**
- - customer:**Long**
- - product:**String**
- - region:**String of length 2**
- - helpful:**Int**
- - verified:**Bool**
- - vine:**String**
- - votes:**Int**

---

## Services
For each entity, CRUD endpoints are provided. Consult the included Redoc spec on the Web Client for information on the API.

---

## Web Client
The Server presents a HTML5+JS Client on the root URI, which allows the user to visualize all server data and realize CRUD operations on all entities. Additionally, it provides an OpenAPI Spec for the API.
