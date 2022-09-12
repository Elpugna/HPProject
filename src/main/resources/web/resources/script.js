var dataTable = null
var section = "customer"
var columns = {
    customer:['id','name','product reviews','update','delete'],
    product:['category','id','title','reviews','update','delete'],
    review:[
          "id",
          "title",
          "rating",
          "date",
          "body",
          "customer",
          "product",
          "region",
          "helpful",
          "verified",
          "vine",
          "votes",
          'update',
          "delete"]
    }

$( document ).ready(function() {
    setTable(section)
    _.forOwn((columns),((v,k,o)=>{
        setupPost(k)
        setupPut(k)
        })
    )
    $('#verifiedNew').checkbox()
    $('#vineNew').checkbox()
    $('#verifiedUpdate').checkbox()
    $('#vineUpdate').checkbox()
});
var baseUri= 'http://localhost:8080/api/'


function setTable(table){
    section=table
    $("#container").html(
        "<div><table id='table'><thead><tr>"+
        columns[table].map(x=>"<td>"+x+"</td>").join(" ")
        +"</tr></thead></table></div>"
        +"<button class='ui primary green icon button' onclick=\"$('#add"
        +_.capitalize(table)
        +"Modal').modal('show')\"><i class='add icon'></i></button>"
    )
    $("#status").text( "- Showing "+_.capitalize(table)+"s")
    loadTable(table,columns[table].map(x=>({data:x})))
}
function showProductReviews(id){
    fetch(baseUri+"product/"+id+"/reviews/all",
        {method:'GET',
           headers: {
            'Content-Type': 'application/json',
          }
        }).then(x=>{
            return x.json()
        }).then(data=>  {
            console.log(data)
            $(`#viewProductModal .content`).html(
                data
                .map(r=> `
                <div class="entry"><h3>Review</h3>
                <div class="reviewData">
                    ${_.toPairs(r).map(p=>"<div><b>"+p.join("</b>")+"</div>").join("<br>")}
                </div>
                </div>`)
                .join("<hr>")
            )
        });
$(`#viewProductModal`).modal('show')
}
function showCustomerReviews(id){
    fetch(baseUri+"customer/"+id+"/reviews/all",
        {method:'GET',
           headers: {
            'Content-Type': 'application/json',
          }
        }).then(x=>{
            return x.json()
        }).then(data=>  {
            console.log(data)
            $(`#viewCustomerModal .content`).html(
                data
                .map(tuple=> `
                <div class="entry"><h3>Product</h3>
                <div class="productData">
                    ${_.toPairs(tuple[1]).map(p=>"<div><b>"+p.join("</b>")+"</div>").join("<br>")}
                </div>
                <details>
                <summary><b>Review</b></summary>
                <div class="reviewData">
                    ${_.toPairs(tuple[0]).map(p=>"<div><b>"+p.join("</b>")+"</div>").join("<br>")}
                </div>
                </details>
                </div>`)
                .join("<hr>")
            )
        });

$(`#viewCustomerModal`).modal('show')
}

async function responseHandler(x){
    console.log(x)
    if(x.status> 205){
        var t = await x.text()
        $('body')
          .toast({
            message: `<b>${x.statusText}:</b>${t}`
          })
    }else{
        $('body')
          .toast({
            message: `<b>${x.statusText}!</b`
          })
            dataTable?.ajax.reload()
            $(`#add${_.capitalize(section)}Modal`).modal('hide')
    }
}
const regex = /"(-?[0-9]+\.{0,1}[0-9]*)"/g

function setupPost(endpoint){
    var form = document.querySelector(`#${endpoint}Form`)
    form.onsubmit = function(event){
        var formData = new FormData(form);
        var checkbox = $(`#${endpoint}Form`).find("input[type=checkbox]")
        $.each(checkbox, function(key, val) {
            formData.append($(val).attr('name'), $(this).is(':checked'))
        })
        fetch(baseUri+endpoint,
        {
           body: JSON.stringify(Object.fromEntries(formData))
            .replace(regex, '$1').replaceAll('"true"','true').replaceAll('"false"','false'),
           method: 'POST',
           headers: {
            'Content-Type': 'application/json',
          }
        }).then(responseHandler);
        return false;
    }
}

function setupPut(endpoint){
    var form = document.querySelector(`#update${_.capitalize(endpoint)}Form`)
    form.onsubmit = function(event){
        var formData = new FormData(form);
        var checkbox = $(`#update${_.capitalize(endpoint)}Form`).find("input[type=checkbox]")
        $.each(checkbox, function(key, val) {
            formData.append($(val).attr('name'), $(this).is(':checked'))
        })
        var data = Object.fromEntries(formData)
        fetch(baseUri+endpoint+"/"+data.id,
        {
           body: JSON.stringify(data)
            .replace(regex, '$1').replaceAll('"true"','true').replaceAll('"false"','false'),
           method: 'PUT',
           headers: {
            'Content-Type': 'application/json',
          }
        }).then(responseHandler);
        return false;
    }
}

function performDelete(entity,id){
    fetch(`${baseUri}${entity}/${id}`,
                {method:'DELETE'})
                .then(responseHandler)
}

function openUpdate(entity,id) {
    fetch(`${baseUri}${entity}/${id}`,
    {method:'GET',
       headers: {
        'Content-Type': 'application/json',
      }
    }).then(x=>{
        return x.json()
    }).then(data=>  {
        _.forIn(data, (v,k,o)=>{
            if((typeof v) =="boolean") $(`#${k}Update`).checkbox(v?"check":"uncheck")
            else document.forms[`update${_.capitalize(entity)}Form`][k].value=v
        })
        $(`#update${_.capitalize(entity)}Modal`).modal("show")
    });
}

function loadTable(endpoint,format){
    dataTable = $('#table').DataTable( {
        serverSide: true,
        ajax: {
            "url":baseUri+endpoint,
            "type":'GET',
            data:function(data){
                var req = {}
                req.page = data.start/data.length;
                req.length= data.length
                return  req;
            },
            dataFilter: function(data){
                var json = jQuery.parseJSON( data );
                console.log(json.data)
                json.data
                .map(c=> {
                    _.set(c,"update",
                        `<button class="ui tertiary yellow button icon" onClick="openUpdate('${section}','${c.id}')">
                            <i class="edit icon"></i>
                         </button>`)
                    _.set(c,"delete",
                        `<button class="ui tertiary red button icon" onClick="performDelete('${section}','${c.id}')">
                            <i class="trash icon"></i>
                         </button>`)
                    if(section=="customer") _.set(c,"product reviews",
                        `<button class="ui tertiary button" onClick="showCustomerReviews('${c.id}')">View
                         </button>`)
                    if(section=="product") _.set(c,"reviews",
                        `<button class="ui tertiary button" onClick="showProductReviews('${c.id}')">View
                         </button>`)
                })
                console.log(json.data)
                json.recordsTotal = json.total;
                json.recordsFiltered = json.total;
                return JSON.stringify( json );
            }
        },
        dataSrc: '',
        "sAjaxDataProp": "data",
        columns: format,
        "scrollY": "550px",
        "scrollCollapse": true,
        "searching":false,
        "ordering":false
    } );
}

function setSpec(){
    Redoc.init('/resources/openapi3_0.yaml', {
      scrollYOffset: 50
    }, document.getElementById('spec-container'))
    $('#specModal').modal('show')
}