<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE html>
<html>
<head>
<title>Demo</title>
<meta content="width=device-width, initial-scale=1" name="viewport">
<link href="https://www.w3schools.com/w3css/4/w3.css" rel="stylesheet">
<link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css" rel="stylesheet">
<style>
    .float-child {
        width: 50%;
        float: left;
        padding: 20px;
        border: 2px solid red;
    }
    .float-child1 {
            width: 50%;
            float: right;
            padding: 20px;
            border: 2px solid blue;
        }
		.grid-container {
			display: grid;
			grid-template-columns: 1fr 1fr;
			grid-gap: 20px;
		}
</style>

<script>
function myFunction(id) {
console.log(id)
  var x = document.getElementById(id);
  if (x.className.indexOf("w3-show") == -1) {
    x.className += " w3-show";
  } else {
    x.className = x.className.replace(" w3-show", "");
  }
}
</script>
</head>
<body>

<div class="grid-container">
<div style="margin:10px;" class="grid-child purple">
	<form:form action="createBuilder" method="post" modelAttribute="builder">
    <h3>Build DIH</h3>

    <div class="w3-container w3-card-4 w3-panel w3-padding-16" style="width:30%">
    <form:label path="cloudProvider"> Choose cloud provider :</form:label>
    <form:select class="w3-select w3-border" id="cloudProvider" name="cloudProvider" path="cloudProvider">
        <form:option value="AWS">AWS</form:option>
        <form:option value="GCP">GCP</form:option>
    </form:select>
    <br>
    <form:label path="region"> Choose region :</form:label>
    <form:select class="w3-select w3-border" id="region" name="region" path="region">
        <form:option value="us-west">us-west</form:option>
    </form:select>
    <br>
    <form:label path="accessKey"> Provide access key :</form:label>
    <form:input class="w3-select w3-border" id="accessKey" name="accessKey" type="text" path="accessKey" />
    <br>
    <form:label path="secretKey"> Provide secret key :</form:label>
    <form:input class="w3-select w3-border" id="secretKey" name="secretKey" type="text" path="secretKey"></form:input>
</div>
<div class="w3-block w3-white w3-left-align" onclick="myFunction('Demo1')" style="padding:0px;cursor:pointer">advanced
    <i class="fa fa-caret-right"> </i></div>
<div class="w3-hide w3-animate-zoom" id="Demo1">
    <!--  <a href="#" class="w3-button w3-block w3-left-align">DIH parameters</a>-->
    <!--   <a href="#" class="w3-button w3-block w3-left-align">DIH parameters</a>-->
    <!--    <a href="#" class="w3-button w3-block w3-left-align">DIH parameters</a>-->
</div>
<div class = "w3-container w3-card-4 w3-panel w3-padding-16" style="width:30%">
<br>

<form:label path="size"> choose size in GB :</form:label>
<form:select class="w3-select w3-border" id="size" name="size" path="size">
    <form:option value="16">16</form:option>
</form:select>
<br>
<form:label path="noOfPartitions"> choose number of partitions :</form:label>
<form:select class="w3-select w3-border" id="noOfPartitions" name="noOfPartitions" path="noOfPartitions">
    <form:option value="2">2</form:option>
</form:select>
<br>
<form:label path="cpu"> choose CPU :</form:label>
<form:select class="w3-select w3-border" id="cpu" name="cpu" path="cpu">
    <form:option value="P3">P3</form:option>
</form:select>
<br>
<input class="w3-btn w3-teal" type="submit" value="BUILD  DIH"/>
</form:form>
</div>


<div id="clusters" class="float-child">
    ${clusters}
</div>

<div id="services" class="float-child1">
     ${services}
</div>

</div>
</body>
</html>