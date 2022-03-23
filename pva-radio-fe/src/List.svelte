<script>
 import { createEventDispatcher } from 'svelte';
 
 export let items;
 export let editable = false;

 const api = config.API_URL;
 
 const dispatch = createEventDispatcher();
 
 const play = function (item){
     fetch (api + "/api/play/" + item.uuid);
     dispatch ('current', {uuid: item.uuid, name: item.name});
 }

 const remove = function (uuid){
     fetch (api + "/api/remove/" + uuid);
     dispatch ('remove', {});
 }

</script>

{#each items as item}
    
    <div>
        <span on:click="{() => play(item)}" class="clickable">
            {#if item.icon !== ""}
                <img src="{item.icon}" style="width:1em;height:1em;" />
            {:else}
                <span style="width: 1em;"></span>
            {/if}
            <span class="overflow" style="width: 12em;">{item.name}</span>
        </span>
        <span class="overflow">
            <a href="{item.url}" target="_blank">
                <img alt="play here" src="phone.png" style="width:1em;height:1em;padding-left: 1em;"/>
            </a>
        </span>
        {#if editable}
            <span class="overflow">
                <img alt="trash" src="trash.png" style="width:1em;height:1em;padding-left: 1em;"
                     on:click="{() => remove(item.uuid)}" class="clickable" />
            </span>
        {/if}
    </div>
{/each}



<style>
 .clickable {
     cursor: pointer;
 }
 span {
     display: inline-block;
 }
 .overflow {
     text-overflow: ellipsis;
     white-space: nowrap;
     overflow: hidden;
 }
</style>
