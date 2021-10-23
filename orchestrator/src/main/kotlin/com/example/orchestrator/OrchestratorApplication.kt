package com.example.orchestrator

import io.temporal.worker.WorkerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class OrchestratorApplication

fun main(args: Array<String>) {
	val appContent = runApplication<OrchestratorApplication>(*args)
	val workerFactory = appContent.getBean(WorkerFactory::class.java)
	val activities = appContent.getBean(OrderActivities::class.java)
	val worker = workerFactory.newWorker(OrderWorkflow.taskList)
	worker.registerWorkflowImplementationTypes(OrderWorkflowImpl::class.java)
	worker.registerActivitiesImplementations(activities)

	workerFactory.start()

	Runtime.getRuntime().addShutdownHook(Thread {
		workerFactory.shutdown()
	})
}
