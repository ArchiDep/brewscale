from locust import HttpUser, constant, task

class Customer(HttpUser):
    wait_time = constant(1)

    @task
    def order_a_flat_white(self):
        self.client.post("/orders", data={"drink": "flat-white"})
