<script>

 import List from './List.svelte'

 const api = config.API_URL;
 
 let term = '';

 let history = [];
 let favorites = [];
 let byname = [];
 let bytag = [];
 let loading = false;

 let current = '';
 let currentName = '';

 const handleCurrent = function (evt){
     current = evt.detail.uuid;
     currentName = evt.detail.name;
 }
 
 const loadHistory = async function (){
     let result =  await fetch(api + "/api/history");
     let loaded = await result.json();
     history = loaded;
 }

 const loadFavorites = async function (){
     let result =  await fetch(api + "/api/favorites");
     let loaded = await result.json();
     favorites = loaded;
 }

 const search = async function (){
     loading = true;
     let result = await fetch (api + "/api/search/" + term)
     let loaded = await result.json ();
     byname = loaded.byname;
     bytag = loaded.bytag;
     loading = false;
 }

 const handleKeypress = function (evt){
     if (evt.keyCode === 13){
         search ();
     }
 }

 const handleRemove = function (evt) {
     loadFavorites ();
 }

 const voteup = function (){
     fetch (api + "/api/voteup/" + current);
     loadFavorites ();
 }

 const kill = function (){
     fetch (api + "/api/kill");
     current = '';
 }

 let histPromise = loadHistory ();
 let favsPromise = loadFavorites ();
</script>



<input type="text" placeholder="e.g. piano" bind:value={term} on:keypress={handleKeypress}/>
<button on:click="{search}">Search for {term}</button>
<div>
    {#if current !== ""}
        <span>Playing: {currentName}</span>
        <button on:click="{voteup}"><img alt="add to favorites" src="heart.png" style="width:1em;"/></button>
    {/if}
    <button on:click="{kill}"><img alt="kill" src="kill.png" style="width:1em;"/></button>
</div>

{#if loading}
    <span>Loading...</span>
{/if}
{#if byname.length !== 0}
    <h2>Found by name:</h2>
    <List items="{byname}" on:current="{handleCurrent}" />
{/if}

{#if byname.length !== 0}
    <h2>Found by tag:</h2>
    <List items="{bytag}"  on:current="{handleCurrent}" />
{/if}

{#await favsPromise}
    <div>waiting...</div>
{:then go}
    <h2>favorites</h2>
    <List items="{favorites}" editable="{true}"  on:current="{handleCurrent}" on:remove="{handleRemove}" />
{:catch error}
    <div>Can't load favorites...</div>
{/await}

{#await histPromise}
    <div>waiting...</div>
{:then go}
    <h2>history</h2>
    <List items="{history}" on:current="{handleCurrent}" />
{:catch error}
    <div>Can't load history...</div>
{/await}



