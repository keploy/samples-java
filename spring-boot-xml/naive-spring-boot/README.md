<h1>Sample java spring boot with xml</h1>
<p>This project is a Spring Boot application that serves a simple REST API endpoint returning XML responses.</p>

<h2>How to Run</h2>
<p>Ensure you have Java 17 installed. Then, follow these steps:</p>
<ol>
<li>Clone the repository </li>
<li>Navigate into the project directory</li>
<li>Build the project: <code>mvn clean install</code></li>
<li>Run the application: <code>mvn spring-boot:run</code></li>
</ol>

<h2>API Endpoints</h2>
<h3>Get User (Returns XML)</h3>
<p>Endpoint: <code>GET /api/user</code></p>

<h3>Testing with Curl</h3>
<p>To make a request using <code>curl</code>, use the following command:</p>
<pre>
<code>curl -X GET -H "Accept: application/xml" http://localhost:8080/api/user</code>
</pre>

<h2>Expected XML Response</h2>
<pre>
<code>
&lt;User&gt;
&lt;name&gt;John Doe&lt;/name&gt;
&lt;age&gt;30&lt;/age&gt;
&lt;phone&gt;0101233333&lt;/phone&gt;
&lt;/User&gt;
</code>
</pre>

<h2>Dependencies</h2>
<ul>
<li>Spring Boot</li>
<li>Spring Web</li>
<li>JAXB for XML serialization</li>
</ul>