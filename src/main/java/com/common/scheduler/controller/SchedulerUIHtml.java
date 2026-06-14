package com.common.scheduler.controller;

public class SchedulerUIHtml {

    public static final String HTML = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Scheduler Dashboard</title>
            <style>
                :root {
                    --primary: #4F46E5;
                    --bg: #111827;
                    --panel-bg: #1F2937;
                    --text: #F9FAFB;
                    --text-muted: #9CA3AF;
                    --success: #10B981;
                    --danger: #EF4444;
                    --border: #374151;
                }
                body {
                    font-family: 'Inter', system-ui, -apple-system, sans-serif;
                    background-color: var(--bg);
                    color: var(--text);
                    margin: 0;
                    padding: 2rem;
                }
                .container {
                    max-width: 1200px;
                    margin: 0 auto;
                }
                h1 {
                    font-weight: 600;
                    margin-bottom: 2rem;
                    color: white;
                }
                .card {
                    background-color: var(--panel-bg);
                    border-radius: 12px;
                    border: 1px solid var(--border);
                    padding: 1.5rem;
                    margin-bottom: 1.5rem;
                    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
                }
                .job-header {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    margin-bottom: 1rem;
                    border-bottom: 1px solid var(--border);
                    padding-bottom: 1rem;
                }
                .job-title {
                    font-size: 1.25rem;
                    font-weight: 600;
                }
                .job-bean {
                    color: var(--text-muted);
                    font-size: 0.875rem;
                    margin-top: 0.25rem;
                }
                table {
                    width: 100%;
                    border-collapse: collapse;
                    margin-top: 1rem;
                }
                th, td {
                    padding: 0.75rem;
                    text-align: left;
                    border-bottom: 1px solid var(--border);
                }
                th {
                    color: var(--text-muted);
                    font-weight: 500;
                    font-size: 0.875rem;
                }
                .badge {
                    padding: 0.25rem 0.75rem;
                    border-radius: 9999px;
                    font-size: 0.75rem;
                    font-weight: 600;
                }
                .badge.active { background-color: rgba(16, 185, 129, 0.2); color: var(--success); }
                .badge.inactive { background-color: rgba(239, 68, 68, 0.2); color: var(--danger); }
                
                button {
                    background-color: var(--primary);
                    color: white;
                    border: none;
                    padding: 0.5rem 1rem;
                    border-radius: 6px;
                    cursor: pointer;
                    font-weight: 500;
                    transition: opacity 0.2s;
                }
                button:hover { opacity: 0.9; }
                button.toggle-inactive { background-color: var(--danger); }
                button.toggle-active { background-color: var(--success); }
            </style>
        </head>
        <body>
            <div class="container">
                <h1>Quartz Scheduler Dashboard</h1>
                <div id="jobs-container">Loading...</div>
            </div>

            <script>
                async function loadJobs() {
                    const res = await fetch('/custom-scheduler/api/jobs');
                    const jobs = await res.json();
                    
                    const container = document.getElementById('jobs-container');
                    container.innerHTML = '';
                    
                    jobs.forEach(job => {
                        const statusClass = job.status === 'ACTIVE' ? 'active' : 'inactive';
                        const toggleText = job.status === 'ACTIVE' ? 'Disable' : 'Enable';
                        const toggleClass = job.status === 'ACTIVE' ? 'toggle-inactive' : 'toggle-active';
                        const newStatus = job.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
                        
                        let triggersHtml = '';
                        job.triggers.forEach(t => {
                            const lastExec = t.lastExecutionDate ? t.lastExecutionDate.replace('T', ' ') : 'Never';
                            const nextExec = t.nextExecutionDate ? t.nextExecutionDate.replace('T', ' ') : 'N/A';
                            const execStatus = t.lastExecutionStatus || 'PENDING';
                            const execClass = execStatus === 'SUCCESS' ? 'color: var(--success)' : (execStatus === 'FAILED' ? 'color: var(--danger)' : '');

                            triggersHtml += `
                                <tr>
                                    <td>${t.jobGroup}</td>
                                    <td>${t.cronExpression}</td>
                                    <td>${t.timezone}</td>
                                    <td style="${execClass}">${execStatus}</td>
                                    <td style="color: var(--text-muted)">${lastExec}</td>
                                    <td style="color: var(--text-muted)">${nextExec}</td>
                                </tr>
                            `;
                        });

                        const card = `
                            <div class="card">
                                <div class="job-header">
                                    <div>
                                        <div class="job-title">${job.jobName} <span class="badge ${statusClass}">${job.status}</span></div>
                                        <div class="job-bean">${job.beanName}.${job.methodName}() | Parallelism: ${job.parallelism}</div>
                                    </div>
                                    <button class="${toggleClass}" onclick="toggleStatus(${job.id}, '${newStatus}')">${toggleText}</button>
                                </div>
                                <table>
                                    <thead>
                                        <tr>
                                            <th>Group</th>
                                            <th>Cron</th>
                                            <th>Timezone</th>
                                            <th>Last Status</th>
                                            <th>Last Execution</th>
                                            <th>Next Execution</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        ${triggersHtml}
                                    </tbody>
                                </table>
                            </div>
                        `;
                        container.innerHTML += card;
                    });
                }

                async function toggleStatus(jobId, newStatus) {
                    await fetch(`/custom-scheduler/api/jobs/${jobId}/status?status=${newStatus}`, { method: 'POST' });
                    loadJobs();
                }

                // Initial load
                loadJobs();
                // Auto refresh every 10 seconds
                setInterval(loadJobs, 10000);
            </script>
        </body>
        </html>
    """;
}
