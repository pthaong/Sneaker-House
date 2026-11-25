(() => {
  'use strict';
  const CFG = window.CATALOG_CONFIG;
  const $ = (s, r=document) => r.querySelector(s);
  const $$ = (s, r=document) => [...r.querySelectorAll(s)];
  const money = n => `${new Intl.NumberFormat('vi-VN').format(Number(n)||0)}đ`;
  const safeImage = value => {
    if (!value || typeof value !== 'string') return '';
    const v=value.trim();
    if (/^https?:\/\//i.test(v)) return v;
    if (v.startsWith('/') && !v.startsWith('//')) return new URL(v, CFG.apiBase).href;
    if (/^[\w./-]+\.(webp|png|jpe?g|avif)$/i.test(v)) return new URL(v, CFG.apiBase + '/').href;
    return '';
  };
  const escapeHtml = s => String(s ?? '').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  let csrf = null;
  async function request(path, options={}) {
    const method=(options.method||'GET').toUpperCase();
    const url=new URL(CFG.apiBase + path);
    if(options.params) Object.entries(options.params).forEach(([k,v])=>{if(v!==''&&v!=null)url.searchParams.set(k,v)});
    const headers={Accept:'application/json'};
    const init={method,headers,credentials:'include'};
    if(method!=='GET'&&method!=='HEAD'){
      if(options.body!==undefined){headers['Content-Type']='application/json';init.body=JSON.stringify(options.body)}
      try {
        if(!csrf){const c=await fetch(CFG.apiBase+'/auth/csrf',{credentials:'include',headers:{Accept:'application/json'}});if(c.ok)csrf=await c.json()}
        if(csrf?.token)headers[csrf.headerName||'X-XSRF-TOKEN']=csrf.token;
      } catch(_) {}
    }
    try {let res=await fetch(url,init);if(res.status===403&&method!=='GET'){csrf=null;try{const c=await fetch(CFG.apiBase+'/auth/csrf',{credentials:'include'});if(c.ok){csrf=await c.json();headers[csrf.headerName||'X-XSRF-TOKEN']=csrf.token;res=await fetch(url,init)}}catch(_){}}
      let data=null;try{data=await res.json()}catch(_){}
      return {ok:res.ok,status:res.status,data};
    } catch(error){return {ok:false,status:0,data:null,error}}
  }
  function toast(message){const node=$('[data-toast]');if(!node)return;node.textContent=message;node.hidden=false;clearTimeout(toast.timer);toast.timer=setTimeout(()=>node.hidden=true,3600)}
  function getList(data){return Array.isArray(data)?data:(data?.content||data?.items||data?.data||[])}
  function detailUrl(p){return `product.html?id=${encodeURIComponent(p.id)}`}
  function productCard(p){
    const price=Number(p.salePrice&&Number(p.salePrice)<Number(p.originalPrice)?p.salePrice:p.originalPrice)||0;
    const original=Number(p.originalPrice)||price;
    const discount=Number(p.discountPercent)|| (original>price?Math.round((original-price)*100/original):0);
    const image=safeImage(p.imageUrl);
    const stock=Number(p.stockQuantity??0);
    const article=document.createElement('article');article.className='pcard'+(stock>0?'':' is-out');
    article.innerHTML=`<a class="pcard__media" href="${detailUrl(p)}">${image?`<img src="${escapeHtml(image)}" alt="${escapeHtml(p.name)}" loading="lazy">`:'<span class="image-fallback">SNEAKER HOUSE</span>'}${discount>0?`<span class="badge">-${discount}%</span>`:''}${stock<=0?'<span class="badge badge--stock">Hết hàng</span>':''}</a><button class="wish" type="button" aria-label="Yêu thích ${escapeHtml(p.name)}" title="Yêu thích" data-wish>♡</button><div class="pcard__body"><span class="pcard__brand">${escapeHtml(p.brand||'Sneaker House')}</span><a class="pcard__name" href="${detailUrl(p)}">${escapeHtml(p.name)}</a><div class="price"><b>${money(price)}</b>${original>price?`<s>${money(original)}</s>`:''}</div>${p.soldCount!=null?`<div class="rate">Đã bán ${Number(p.soldCount)||0}</div>`:''}<div class="pcard__act"><a class="btn" href="${detailUrl(p)}">${stock>0?'Chọn size':'Xem chi tiết'}</a></div></div>`;
    article.querySelector('[data-wish]').addEventListener('click',()=>toast('API yêu thích chưa có trong hợp đồng Backend hiện tại.'));
    return article;
  }
  async function loadHeader(){
    const [cats,brands,user]=await Promise.all([request('/categories/tree'),request('/brands'),request('/profile')]);
    const categoryList=getList(cats.data),brandList=getList(brands.data);
    const menu=$('[data-menu]'),menuBtn=$('[data-menu-btn]'),account=$('[data-account-list]');
    if(menuBtn&&account){if(user.ok&&user.data?.email){$('[data-user-name]').textContent=user.data.fullName||'Tài khoản';account.innerHTML=`<li><a href="../fe-account/profile.html">Thông tin cá nhân</a></li><li><a href="../fe-account/account-security.html">Bảo mật</a></li><li><button type="button" data-logout>Đăng xuất</button></li>`;account.querySelector('[data-logout]').addEventListener('click',async()=>{const r=await request('/auth/logout',{method:'POST'});if(r.ok)location.href='../fe-home/index.html';else toast('Không thể đăng xuất.');})}else{account.innerHTML=`<li><a href="${CFG.loginUrl}">Đăng nhập</a></li><li><a href="${CFG.loginUrl.replace('login.html','register.html')}">Đăng ký</a></li>`}menuBtn.addEventListener('click',()=>{account.hidden=!account.hidden;menuBtn.setAttribute('aria-expanded',String(!account.hidden))});document.addEventListener('click',e=>{if(!menu.contains(e.target))account.hidden=true})}
    const mega=$('[data-mega]'),megaBtn=$('[data-mega-btn]'),scrim=$('[data-mega-scrim]');
    if(mega&&megaBtn){const main=$('[data-mega-main]'),body=$('[data-mega-body]'),side=$('[data-mega-brands]');const open=v=>{mega.hidden=!v;scrim.hidden=!v;megaBtn.setAttribute('aria-expanded',String(v))};
      const show=(c)=>{body.innerHTML=`<h3>${escapeHtml(c.name)}</h3><div class="chips">${(c.children||[]).map(x=>`<a href="products.html?categoryId=${x.id}">${escapeHtml(x.name)}</a>`).join('')}</div><a href="products.html?categoryId=${c.id}">Xem tất cả ${escapeHtml(c.name)}</a>`};
      main.innerHTML=categoryList.map((c,i)=>`<li><button type="button" data-cat-index="${i}">${escapeHtml(c.name)}</button></li>`).join('');main.querySelectorAll('button').forEach((b,i)=>b.addEventListener('mouseenter',()=>show(categoryList[i])));if(categoryList[0])show(categoryList[0]);
      if(side)side.innerHTML=`<h3>Thương hiệu</h3><div class="chips">${brandList.slice(0,8).map(b=>`<a href="search.html?brandId=${b.id}">${escapeHtml(b.name)}</a>`).join('')}</div>`;const priceSlot=$('[data-mega-prices]');if(priceSlot)priceSlot.innerHTML='<h3>Khoảng giá</h3><div class="chips"><a href="search.html?minPrice=0&maxPrice=1000000">Dưới 1 triệu</a><a href="search.html?minPrice=1000000&maxPrice=2000000">1–2 triệu</a><a href="search.html?minPrice=2000000&maxPrice=3000000">2–3 triệu</a><a href="search.html?minPrice=3000000">Trên 3 triệu</a></div>';
      megaBtn.addEventListener('click',()=>open(mega.hidden));scrim?.addEventListener('click',()=>open(false));document.addEventListener('keydown',e=>{if(e.key==='Escape')open(false)});
    }
    $('[data-link="cart"]')?.addEventListener('click',e=>{e.preventDefault();toast('Trang giỏ hàng thuộc FE2; source hiện tại chưa cung cấp đường dẫn trang giỏ.');});
    $('[data-link="wishlist"]')?.addEventListener('click',e=>{e.preventDefault();toast('Chức năng yêu thích chưa có API trong Backend hiện tại.');});
    $('[data-search]')?.addEventListener('submit',e=>{e.preventDefault();const q=$('[data-search] input[name="q"]').value.trim();location.href='search.html'+(q?'?keyword='+encodeURIComponent(q):'')});
    return {categories:categoryList,brands:brandList,user:user.ok?user.data:null};
  }
  window.Catalog={ $, $$, money, safeImage, escapeHtml, request, toast, getList, productCard, detailUrl, loadHeader };
})();
