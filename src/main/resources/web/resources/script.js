var dataTable = null
var section = "customer"
var columns = {
    customer:['id','name','update','delete'],
    product:['id','title','category','update','delete'],
    review:[
          "id",
          "title",
          "rating",
          "date",
          "body",
          "customer",
          "product",
          "region",
          "votes",
          "helpful",
          "verified",
          "vine",
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
function showProduct(id){
Promise.all([
  (fetch(baseUri+"product/"+id+"/reviews/all",
          {method:'GET',
             headers: {
              'Content-Type': 'application/json',
            }
          }).then(x=>x.json())),
  (fetch(baseUri+"product/"+id,
          {method:'GET',
             headers: {
              'Content-Type': 'application/json',
            }
          }).then(x=>x.json()))
    ])
    .then((data)=>  {
            $('body').modal({
                title:`${data[1].title} Details <small style="float:right;color:#aaa">${data[1].category}</small>`,
                class: '',
                closeIcon: true,
                content: "<div class='ui segments'>"+data[0].map(r=>"<div class='ui segment'>"+makeReview(r)+"</div>").join("")+"</div>",
                actions: []
            }).modal('show');
        });
}

function makeRating(rating){
    return `<div class="ui yellow rating disabled">
        ${'<i class="star icon active"></i>'.repeat(rating)+
          '<i class="star icon"></i>'.repeat(5-rating)}
      </div>`
}

function makeReview(r){
return `<div class="comment">
            <div class="content">
              <div class="metadata">
                          <b class="author">
                              ${r.title}
                          </b>
                ${r.verified?"<i class='check icon' title='Verified Purchase'></i>":""}
               <div class="ui label" title="Review Votes">
                   <i class="vote yea icon"></i> ${r.votes}
               </div>
               <div class="ui label" title="Helpful Votes">
                  <i class="hands helping icon"></i> ${r.helpful}
                </div>
                ${r.vine?"<i class='pagelines icon' title='Part of the Vine Program'></i>":""}
              </div>
              <div class="text">
              <div>
                 ${makeRating(r.rating)}
              </div>
                ${r.body}
              </div>
              <div class="actions">
                <i class="${r.region.toLowerCase()} flag" title="${r.region.toUpperCase()} Marketplace Purchase"></i>
                <span class="date">${r.date} &nbsp;</span>
              <span style="float:right"><small>User <a onClick="showCustomer(${r.customer})">#${r.customer}</a></small>  </span>
              </div>
            </div>
          </div>`
}

var productWithReviews = (tuple)=> `<div class="ui fluid card">
                                           <div class="content">
                                             <div class="header">${tuple[1].title}</div>
                                             <div class="meta">${tuple[1].category}</div>
                                             <div class="description">
                                             <div class="ui raised segment">
                                                 ${makeReview(tuple[0])}
                                             </div>
                                             </div>
                                           </div>
                                           <div class="extra content">
                                               <b>Product:</b> <a onClick="showProduct('${tuple[1].id}')">#${tuple[1].id}</a>
                                             </div>
                                         </div>`

function showCustomer(id){
    fetch(baseUri+"customer/"+id+"/reviews/all",
        {method:'GET',
           headers: {
            'Content-Type': 'application/json',
          }
        }).then(x=>{
            return x.json()
        }).then(data=>  {
            $('body').modal({
                title: 'Customer Details',
                class: '',
                closeIcon: true,
                content: data.map(productWithReviews).join("<hr>"),
                actions: []
            }).modal('show');
        });
}

function showReview(id){
    fetch(baseUri+"review/"+id,
        {method:'GET',
           headers: {
            'Content-Type': 'application/json',
          }
        }).then(x=>{
            return x.json()
        }).then(data=>  {
            $('body').modal({
                title: `Review Details <small style="float:right;color:#aaa">#<i>${data.id}</i></small>`,
                class: '',
                closeIcon: true,
                content: makeReview(data),
                actions: []
            }).modal('show');
        });
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
        $('#customerToUpdate').val(data.customer)
        $('#productToUpdate').val(data.product)
        $(`#update${_.capitalize(entity)}Modal`).modal("show")
    });
}

function updateReviewCustomer(){
    fetch(baseUri+"review/"+document.forms["updateReviewForm"]["id"].value+"/updateCustomer",
            {
               body: $('#customerToUpdate').val(),
               method: 'PUT',
               headers: {
                'Content-Type': 'application/json',
              }
            }).then(responseHandler)
}
function updateReviewProduct(){
    fetch(baseUri+"review/"+document.forms["updateReviewForm"]["id"].value+"/updateProduct",
            {
               body: $('#productToUpdate').val(),
               method: 'PUT',
               headers: {
                'Content-Type': 'application/json',
              }
            }).then(responseHandler)
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
                    if(section=="customer") _.set(c,"id",`<a onClick="showCustomer('${c.id}')">${c.id}</a>`)
                    if(section=="product")  _.set(c,"id",`<a onClick="showProduct('${c.id}')">${c.id}</a>`)

                    if(section=="review"){
                        _.set(c,"id",`<a onClick="showReview('${c.id}')">${c.id}</a>`)
                        _.set(c,"rating", makeRating(c.rating))
                        _.set(c,"region",`<i class="${c.region.toLowerCase()} flag"></i>`)
                        _.set(c,"product",`<a onClick="showProduct('${c.product}')">${c.product}</a>`)
                        _.set(c,"customer",`<a onClick="showCustomer('${c.customer}')">${c.customer}</a>`)
                    }
                })
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